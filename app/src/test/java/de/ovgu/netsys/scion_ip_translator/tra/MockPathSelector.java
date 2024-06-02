package de.ovgu.netsys.scion_ip_translator.tra;

import org.scion.jpan.Path;
import org.scion.jpan.RequestPath;

import java.net.InetAddress;

public class MockPathSelector implements PathSelector {
    @Override
    public long getLocalIsdAs() {
        return (1L << 48) | 0xfc00L;
    }

    @Override
    public RequestPath lookup(long dstIsdAs, InetAddress dstHost, int dstPort) {
        return null;
    }
}
