package de.ovgu.netsys.scion_ip_translator.tra;

import junit.framework.TestCase;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv6HeaderTest extends TestCase {
    static final byte[] pkt = {
            0x60, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x11, 0x40,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
    };

    public void testReadWrite() {
        ByteBuffer buf = ByteBuffer.wrap(pkt);
        IPv6Header hdr = new IPv6Header();

        assertEquals(40, hdr.read(buf));
        assertEquals(40, buf.position());
        assertEquals(6, hdr.version);
        assertEquals(0, hdr.trafficClass);
        assertEquals(0, hdr.flowLabel);
        assertEquals(12, hdr.payloadLength);
        assertEquals(17, hdr.nextHeader);
        assertEquals(64, hdr.hopLimit);
        try {
            assertEquals(Inet6Address.getByName("::1"), hdr.src);
            assertEquals(Inet6Address.getByName("::2"), hdr.dst);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer out = ByteBuffer.allocate(40);
        assertEquals(40, hdr.write(out));
        assertEquals(40, out.position());
        buf.position(0);
        out.position(0);
        assertEquals(buf, out);
    }
}