package de.ovgu.netsys.scion_ip_translator.tra;

import android.net.ipsec.ike.TunnelModeChildSessionParams;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv6Header {
    public byte version;
    public byte trafficClass;
    public int flowLabel;
    public short payloadLength;
    public byte nextHeader;
    public byte hopLimit;
    public Inet6Address src;
    public Inet6Address dst;

    int read(ByteBuffer buf) {
        int b0 = buf.getInt();
        version = (byte)((b0 >> 28) & 0xf);
        trafficClass = (byte)((b0 >> 20) & 0xff);
        flowLabel = b0 & 0xfffff;
        payloadLength = buf.getShort();
        nextHeader = buf.get();
        hopLimit = buf.get();
        try {
            byte[] addrBytes = new byte[16];
            buf.get(addrBytes, 0, 16);
            src = (Inet6Address) Inet6Address.getByAddress(addrBytes);
            buf.get(addrBytes, 0, 16);
            dst = (Inet6Address) Inet6Address.getByAddress(addrBytes);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return 40;
    }

    int write(ByteBuffer buf) {
        int b0 = ((int)(version & 0xf) << 28)
                | (((int)trafficClass & 0xff) << 20)
                | (flowLabel & 0xfffff);
        buf.putInt(b0);
        buf.putShort(payloadLength);
        buf.put(nextHeader);
        buf.put(hopLimit);
        buf.put(src.getAddress(), 0, 16);
        buf.put(dst.getAddress(), 0, 16);
        return 40;
    }
}
