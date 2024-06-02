package de.ovgu.netsys.scion_ip_translator.tra;

import java.net.Inet4Address;

public class ScionAddress {
    public int isd;
    public long asn;
    public int prefix;
    public int subnet;
    public boolean isIPv4;
    public Inet4Address interface4;
    public Long interface6;

    public static ScionAddress MakeIPv4(int isd, long asn, Inet4Address host) {
        ScionAddress a = new ScionAddress();
        a.isd = isd;
        a.asn = asn;
        a.isIPv4 = true;
        a.interface4 = host;
        return a;
    }

    public static ScionAddress MakeIPv6(int isd, long asn, int prefix, int subnet, Long host) {
        ScionAddress a = new ScionAddress();
        a.isd = isd;
        a.asn = asn;
        a.prefix = prefix;
        a.subnet = subnet;
        a.isIPv4 = false;
        a.interface6 = host;
        return a;
    }
}
