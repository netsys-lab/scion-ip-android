package de.ovgu.netsys.scion_ip_translator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public class ScionTranslatorService extends VpnService implements Handler.Callback{
    private final String TAG = ScionTranslatorService.class.getSimpleName();
    public static final String ACTION_CONNECT = "de.ovgu.netsys.scion_ip_translator.START";
    public static final String ACTION_DISCONNECT = "de.ovgu.netsys.scion_ip_translator.STOP";
    private Handler mHandler;
    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }
    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();
    private AtomicInteger mNextConnectionId = new AtomicInteger(1);
    private PendingIntent mConfigureIntent;

    @Override
    public void onCreate() {
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        mConfigureIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            connect();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        disconnect();
    }

    @Override
    public boolean handleMessage(Message message) {
        Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        if (message.what != R.string.disconnected) {
            updateForegroundNotification(message.what);
        }
        return true;
    }

    private void connect() {
        updateForegroundNotification(R.string.connecting);
        mHandler.sendEmptyMessage(R.string.connecting);

        // Extract information from the shared preferences.
        final SharedPreferences prefs = getSharedPreferences(MainActivity.Prefs.NAME, MODE_PRIVATE);
        final String bindAddress = prefs.getString(MainActivity.Prefs.BIND_ADDRESS, "10.0.2.15");
        final int endHostPort = prefs.getInt(MainActivity.Prefs.END_HOST_PORT, 30041);
        final String daemon = prefs.getString(MainActivity.Prefs.DAEMON, "10.0.2.2:30255");

        startTranslation(new ScionTranslator(
                this, mNextConnectionId.getAndIncrement(),
                bindAddress, endHostPort, daemon));
    }

    private void startTranslation(final ScionTranslator translator) {
        // Replace any existing thread with the new one.
        final Thread thread = new Thread(translator, "TranslatorThread");
        setConnectingThread(thread);

        // Handler to mark as connected once onEstablish is called.
        translator.setConfigureIntent(mConfigureIntent);
        translator.setOnEstablishListener(tunInterface -> {
            mHandler.sendEmptyMessage(R.string.connected);
            mConnectingThread.compareAndSet(thread, null);
            setConnection(new Connection(thread, tunInterface));
        });
        thread.start();
    }

    private void setConnectingThread(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setConnection(final Connection connection) {
        final Connection oldConnection = mConnection.getAndSet(connection);
        if (oldConnection != null) {
            try {
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing TUN interface", e);
            }
        }
    }

    private void disconnect() {
        mHandler.sendEmptyMessage(R.string.disconnected);
        setConnectingThread(null);
        setConnection(null);
        stopForeground(true);
    }

    private void updateForegroundNotification(final int message) {
        final String NOTIFICATION_CHANNEL_ID = "SCION";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT));
        startForeground(1, new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.scion_logo_gradient_notagline)
                .setContentText(getString(message))
                .setContentIntent(mConfigureIntent)
                .build());
    }
}