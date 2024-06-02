package de.ovgu.netsys.scion_ip_translator.tra;

import org.scion.jpan.RequestPath;

import java.net.InetAddress;

public interface PathSelector {
    long getLocalIsdAs();
    RequestPath lookup(long dstIsdAs, InetAddress dstHost, int dstPort);
}
