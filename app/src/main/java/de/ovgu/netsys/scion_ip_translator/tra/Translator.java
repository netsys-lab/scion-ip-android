package de.ovgu.netsys.scion_ip_translator.tra;

import static java.lang.Byte.compareUnsigned;

import androidx.annotation.NonNull;

import org.scion.jpan.RequestPath;
import org.scion.jpan.ResponsePath;
import org.scion.jpan.internal.InternalConstants;
import org.scion.jpan.internal.ScionHeaderParser;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Translator {
    public static final long SCION_PREFIX = 0xfc;
    public static final int SCION_PREFIX_LEN = 8;
    public static final int SUBNET_BITS = 8;
    public static final byte NEXT_HDR_TCP = 6;
    public static final byte NEXT_HDR_UDP = 17;

    @NonNull
    public static Inet6Address mapIPv4(int isd, long asn, Inet4Address host) throws IllegalArgumentException {
        if ((isd < 0) || (isd > (1 << 12)))
            throw new IllegalArgumentException("ISD cannot be encoded");

        long encodedAsn = 0;
        if (asn < (1L << 19)) {
            encodedAsn = asn;
        } else if (0x2_0000_0000L <= asn && asn <= 0x2_0007_ffffL) {
            encodedAsn = (1L << 19) | (asn & 0x7ffffL);
        } else {
            throw new IllegalArgumentException("ASN cannot be encoded");
        }

        Long prefix;
        prefix = (SCION_PREFIX << (64 - SCION_PREFIX_LEN)) | ((long)isd << 44) | (encodedAsn << 24);

        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(prefix);
        buf.putInt(0xffff);
        buf.put(host.getAddress());

        try {
            return (Inet6Address) Inet6Address.getByAddress(buf.array());
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static ScionAddress unmapIPv6(Inet6Address ip) throws IllegalArgumentException {
        ByteBuffer buf = ByteBuffer.wrap(ip.getAddress()).order(ByteOrder.BIG_ENDIAN);
        Long prefix = buf.getLong();
        Long host = buf.getLong();

        int isd = (int)((prefix >> 44) & 0xfff);
        long asn = (prefix >> 24) & 0xfffff;
        int localPrefix = (int)(prefix >> (64 + SUBNET_BITS)) & ~(~0 << (24 - SUBNET_BITS));
        int subnet = (int)(prefix & ~(~0 << SUBNET_BITS));

        if ((asn & (1L << 19)) != 0) {
            asn = 0x2_0000_0000L | (asn & 0x7ffffL);
        }

        if (((host & (0xffffffffL << 32)) == (0x0000ffffL << 32)) && localPrefix == 0 && subnet == 0) {
            ByteBuffer ip4 = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putInt((int)(host & 0xffffffff));
            Inet4Address interface4 = null;
            try {
                interface4 = (Inet4Address) Inet4Address.getByAddress(ip4.array());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            return ScionAddress.MakeIPv4(isd, asn, interface4);
        } else {
            return ScionAddress.MakeIPv6(isd, asn, localPrefix, subnet, host);
        }
    }

    public static InetSocketAddress translateEgress(
            ByteBuffer in, ByteBuffer out, InetAddress hostIp, PathSelector pathSel)
    {
        if (in.remaining() < 44) return null; // IPv6 header + L4 ports

        // Parse IP header
        IPv6Header ipHdr = new IPv6Header();
        ipHdr.read(in);
        int dstPort = in.getShort(in.position() + 2);

        if (ipHdr.version != 6)
            return null;
        if (ipHdr.nextHeader != NEXT_HDR_TCP && ipHdr.nextHeader != NEXT_HDR_UDP)
            return null;

        if (compareUnsigned(ipHdr.dst.getAddress()[0], (byte)SCION_PREFIX) != 0)
            return null;

        ScionAddress sciAddr = null;
        try {
            sciAddr = unmapIPv6(ipHdr.dst);
        } catch (IllegalArgumentException e) {
            return null;
        }
        InetAddress dst;
        if (sciAddr.isIPv4)
            dst = sciAddr.interface4;
        else
            dst = ipHdr.dst;

        // Path lookup
        final long localIsdAsn = pathSel.getLocalIsdAs();
        final long remoteIsdAsn = ((long)sciAddr.isd << 48) | sciAddr.asn;
        InetSocketAddress nextHop;
        byte[] rawPath;
        if (remoteIsdAsn == localIsdAsn) {
            rawPath = new byte[0];
            nextHop = new InetSocketAddress(dst, dstPort);
        } else {
            RequestPath path = pathSel.lookup(remoteIsdAsn, ipHdr.dst, dstPort);
            if (path == null) return null;
            try {
                nextHop = path.getFirstHopAddress();
            } catch (UnknownHostException e) {
                return null;
            }
            rawPath = path.getRawPath();
        }

        // New SCION packet
        ScionHeaderParser.write(out,
                in.remaining(),
                rawPath.length,
                localIsdAsn,
                hostIp.getAddress(),
                remoteIsdAsn,
                dst.getAddress(),
                InternalConstants.HdrTypes.UDP,
                0
        );
        ScionHeaderParser.writePath(out, rawPath);
        out.put(in); // L4 header and payload

        return nextHop;
    }

    public static boolean translateIngress(ByteBuffer in, ByteBuffer out, Inet6Address tunIp) {
        // Parse SCION
        if (ScionHeaderParser.validate(in) != null) {
            return false;
        }
        final ResponsePath path = ScionHeaderParser.extractResponsePath(in, null);
        final byte nextHdr = in.get(4);
        if (nextHdr != NEXT_HDR_TCP && nextHdr != NEXT_HDR_UDP) return false;
        final long localIsdAs = path.getLocalIsdAs();
        final long remoteIsdAs = path.getRemoteIsdAs();

        // Translate source and destination addresses
        Inet6Address dst, src;
        try {
            if (path.getLocalAddress() instanceof Inet4Address) {
                dst = mapIPv4(
                        (int) ((localIsdAs >> 48) & 0xffff), localIsdAs & ~(~0L << 48),
                        (Inet4Address) path.getLocalAddress());
            } else {
                dst = (Inet6Address) path.getLocalAddress();
            }
            if (path.getRemoteAddress() instanceof Inet4Address) {
                src = mapIPv4(
                        (int) ((remoteIsdAs >> 48) & 0xffff), remoteIsdAs & ~(~0L << 48),
                        (Inet4Address) path.getRemoteAddress());
            } else {
                src = (Inet6Address) path.getRemoteAddress();
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!dst.equals(tunIp)) return false;

        // Create nex IPv6 header
        IPv6Header ipHdr = new IPv6Header();
        ipHdr.version = 6;
        ipHdr.trafficClass = 0;
        ipHdr.flowLabel = 0;
        ipHdr.payloadLength = 0;
        ipHdr.nextHeader = nextHdr;
        ipHdr.hopLimit = 64;
        ipHdr.src = src;
        ipHdr.dst = dst;
        ipHdr.write(out);
        // Copy L4 header and payload
        in.position(ScionHeaderParser.extractHeaderLength(in));
        out.put(in);

        return true;
    }
}
