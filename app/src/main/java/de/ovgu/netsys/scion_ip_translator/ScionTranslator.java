package de.ovgu.netsys.scion_ip_translator;

import static android.system.OsConstants.AF_INET;

import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.scion.jpan.Scion;
import org.scion.jpan.ScionRuntimeException;
import org.scion.jpan.ScionService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import de.ovgu.netsys.scion_ip_translator.tra.JpanPathSelector;
import de.ovgu.netsys.scion_ip_translator.tra.Translator;

public class ScionTranslator implements Runnable {
    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    private static final int MAX_PACKET_SIZE = 4096;

    private final VpnService mService;
    private final int mConnectionId;
    private InetSocketAddress mBind;
    private String mBootstrapServer;
    private String mRemoteDaemon;
    private boolean mConnectToDaemon;

    private PendingIntent mConfigureIntent;
    private OnEstablishListener mOnEstablishListener;

    ScionTranslator(VpnService service, int connId) {
        mService = service;
        mConnectionId = connId;

        // TODO: Get configuration
        mBind = new InetSocketAddress("192.168.200.25", 30041);
        mBootstrapServer = "192.168.200.253:8041";
        mRemoteDaemon = "192.168.200.253:30255";
        mConnectToDaemon = true;
    }

    private final String getTag() {
        return ScionTranslator.class.getSimpleName() + "[" + mConnectionId + "]";
    }

    public void setConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    public void setOnEstablishListener(OnEstablishListener listener) {
        mOnEstablishListener = listener;
    }

    @Override
    public void run() {
        ScionService scionService;
        try {
            if (mConnectToDaemon) {
                Log.i(getTag(), String.format("Connecting to daemon at %s", mRemoteDaemon));
                scionService = Scion.newServiceWithDaemon(mRemoteDaemon);
            }
            else {
                Log.i(getTag(), String.format("Bootstrapping from %s", mBootstrapServer));
                scionService = Scion.newServiceWithBootstrapServer(mBootstrapServer);
            }
        } catch (ScionRuntimeException e) {
            Log.e(getTag(), "Cannot connect SCION service", e);
            return;
        }

        try {
            Log.i(getTag(), "Starting translation");
            runTranslation(scionService);
            Log.i(getTag(), "Translator exiting");

        } catch (InterruptedException e) {
            Log.i(getTag(), "Translator exiting");
        } catch  (IOException | ScionRuntimeException e) {
            Log.e(getTag(), "Translator failed", e);
        }
    }

    private void runTranslation(ScionService scionService) throws IOException, InterruptedException {
        final String TAG = getTag();
        JpanPathSelector pathSel = new JpanPathSelector(scionService, !mConnectToDaemon);

        // Configure TUN interface
        final long isdAsn = scionService.getLocalIsdAs();
        Inet6Address tunAddr;
        if (mBind.getAddress() instanceof Inet4Address) {
            tunAddr = Translator.mapIPv4(
                    (int)((isdAsn >> 48) & 0xffff), isdAsn & ~(~0 << 48),
                    (Inet4Address) mBind.getAddress());
        } else {
            tunAddr = (Inet6Address) mBind.getAddress();
        }
        Log.i(TAG, String.format("Bind address: %s", mBind));
        Log.i(TAG, String.format("TUN address: %s", tunAddr));
        final ParcelFileDescriptor tun = configureTun(isdAsn, tunAddr);

        try (DatagramChannel disp = DatagramChannel.open()) {
            // Configure dispatcher socket
            disp.bind(mBind);
            disp.configureBlocking(false);
            if (!mService.protect(disp.socket())) {
                throw new IllegalStateException("Cannot protect dispatcher socket");
            }

            FileInputStream tunIn = new FileInputStream(tun.getFileDescriptor());
            FileOutputStream tunOut = new FileOutputStream(tun.getFileDescriptor());

            // doesn't work: can't select on file streams or file descriptors
            // Selector selector = Selector.open();
            // SelectionKey dispKey = disp.register(selector, SelectionKey.OP_READ);

            // Main loop
            ByteBuffer in = ByteBuffer.allocate(MAX_PACKET_SIZE);
            ByteBuffer out = ByteBuffer.allocate(MAX_PACKET_SIZE);
            while (true) {
                boolean idle = true;

                // IPv6 -> SCION
                int length = tunIn.read(in.array());
                if (length > 0) {
                    //Log.d(TAG, String.format("Received packet from TUN interface (%d bytes)", length));
                    in.limit(length);

                    InetSocketAddress nextHop = Translator.translateEgress(
                            in, out, mBind.getAddress(), mBind.getPort(), pathSel);
                    if (nextHop != null) {
                        if (nextHop.getAddress().getClass() == mBind.getAddress().getClass()) {
                            //Log.d(TAG, "Output: " + Translator.hexdump(out.array(), out.limit()));
                            disp.send(out, nextHop);
                        } else {
                            Log.d(TAG, "host address type mismatch");
                        }
                    } else {
                        //Log.d(TAG, "dropped packet");
                    }

                    in.clear();
                    out.clear();
                    idle = false;
                }

                // SCION -> IPv6
                SocketAddress from = disp.receive(in);
                if (from != null) {
                    length = in.position();
                    in.limit(length);
                    //Log.d(TAG, String.format("Received packet from %s (%d bytes)", from, length));

                    if (Translator.translateIngress(in, out, tunAddr)) {
                        //Log.d(TAG, "Output: " + Translator.hexdump(out.array(), out.limit()));
                        tunOut.write(out.array(), 0, out.limit());
                    } else {
                        //Log.d(TAG, "dropped packet");
                    }

                    in.clear();
                    out.clear();
                    idle = false;
                }

                if (idle) {
                    Thread.sleep(100);
                }
            }
        }
    }

    private ParcelFileDescriptor configureTun(long isdAsn, Inet6Address tunAddr) {
        ScionTranslatorService.Builder builder = mService.new Builder();
        builder.setBlocking(false);
        builder.setMtu(1280); // minimum for IPv6
        builder.addAddress(tunAddr, 64);
        // TODO: Make sure everything not from fc00::/8 is still routed normally
        builder.addRoute("fc00::", 8);
        builder.allowFamily(AF_INET);
        builder.setSession(String.format("AS%d-%d", ((isdAsn >> 48) & 0xffff), isdAsn & ~(~0 << 48)));
        builder.setConfigureIntent(mConfigureIntent);

        final ParcelFileDescriptor tun;
        synchronized (mService) {
            tun = builder.establish();
            if (mOnEstablishListener != null) {
                mOnEstablishListener.onEstablish(tun);
            }
        }
        Log.i(getTag(), String.format("New interface: %s", tun));
        return tun;
    }
}
