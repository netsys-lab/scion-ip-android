package de.ovgu.netsys.scion_ip_translator.tra;

import junit.framework.TestCase;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class TranslatorTest extends TestCase {

    public void testMapIPv4()  {
        try {
            Inet6Address mapped = Translator.mapIPv4(
                    1, 0xfc00L, (Inet4Address) Inet4Address.getByName("10.0.0.1"));
            assertEquals(Inet6Address.getByName("fc00:10fc::ffff:a00:1"), mapped);

            mapped = Translator.mapIPv4(
                    1, 0x2_0000_0000L, (Inet4Address) Inet4Address.getByName("10.0.0.1"));
            assertEquals(Inet6Address.getByName("fc00:1800::ffff:a00:1"), mapped);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void testUnmapIPv6() {
        try {
            ScionAddress sci = null;
            sci = Translator.unmapIPv6((Inet6Address) Inet6Address.getByName("fc00:10fc::ffff:a00:1"));
            assertEquals(1, sci.isd);
            assertEquals(0xfc00, sci.asn);
            assertEquals(0, sci.prefix);
            assertEquals(0, sci.subnet);
            assertEquals(true, sci.isIPv4);
            assertEquals((Inet4Address) Inet4Address.getByName("10.0.0.1"), sci.interface4);

            sci = Translator.unmapIPv6((Inet6Address) Inet6Address.getByName("fc00:1800::ffff:a00:1"));
            assertEquals(1, sci.isd);
            assertEquals(0x2_0000_0000L, sci.asn);
            assertEquals(0, sci.prefix);
            assertEquals(0, sci.subnet);
            assertEquals(true, sci.isIPv4);
            assertEquals((Inet4Address) Inet4Address.getByName("10.0.0.1"), sci.interface4);

            sci = Translator.unmapIPv6((Inet6Address) Inet6Address.getByName("fc01:fc:100:ff20::1"));
            assertEquals(16, sci.isd);
            assertEquals(0xfc01L, sci.asn);
            assertEquals(0xff, sci.prefix);
            assertEquals(0x20, sci.subnet);
            assertEquals(false, sci.isIPv4);
            assertEquals((Long)1L, sci.interface6);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static final byte[] egressIPv6 = {
            // ip version 6
            0x60, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x11, 0x40,
            // src: fc00:10fc::ffff:a00:20f
            (byte)0xfc, (byte)0x00, (byte)0x10, (byte)0xfc, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x0a, (byte)0x00, (byte)0x02, (byte)0x0f,
            // dst: fc00:10fc::ffff:a00:210
            (byte)0xfc, (byte)0x00, (byte)0x10, (byte)0xfc, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x0a, (byte)0x00, (byte)0x02, (byte)0x10,
            // udp: 8192 -> 80
            0x20, 0x00, 0x00, 0x50, 0x00, 0x0C, 0x04, (byte)0xD5,
            // payload: TEST
            0x54, 0x45, 0x53, 0x54
    };

    public void testTranslateEgress() throws UnknownHostException {
        ByteBuffer in = ByteBuffer.wrap(egressIPv6);
        ByteBuffer out = ByteBuffer.allocate(128);

        InetAddress host = Inet4Address.getByName("10.0.2.15");
        PathSelector pathSel = new MockPathSelector();
        InetSocketAddress nextHop = Translator.translateEgress(in, out, host, pathSel);

        assertEquals(new InetSocketAddress(Inet4Address.getByName("10.0.2.16"), 80), nextHop);
        assertEquals(12 + 24 + 8 + 4, out.limit());
    }

    static final byte[] ingressScion = {
        0x00, 0x00, 0x00, 0x01, 0x11, 0x12, 0x00, 0x0C,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, (byte)0xFC, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, (byte)0xFC, 0x01, 0x0A, 0x00, 0x02, 0x0F,
        0x0A, 0x00, 0x02, 0x10, 0x00, 0x00, 0x20, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x66, 0x5C, (byte)0xBE, 0x6D,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x50, 0x20, 0x00, 0x00, 0x0C, 0x27, (byte)0xC9,
        0x54, 0x45, 0x53, 0x54
    };

    public void testTranslateIngress() throws UnknownHostException, IllegalArgumentException {
        ByteBuffer in = ByteBuffer.wrap(ingressScion);
        ByteBuffer out = ByteBuffer.allocate(128);

        Inet4Address host = (Inet4Address) Inet4Address.getByName("10.0.2.15");
        Inet6Address tunIP = Translator.mapIPv4(1, 0xfc00L, host);

        assertTrue(Translator.translateIngress(in, out, tunIP));
        assertEquals(40 + 8 + 4, out.limit());
    }
}