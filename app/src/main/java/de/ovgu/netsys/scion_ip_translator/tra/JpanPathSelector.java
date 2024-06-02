package de.ovgu.netsys.scion_ip_translator.tra;

import org.scion.jpan.RequestPath;
import org.scion.jpan.ScionService;

import java.net.InetAddress;
import java.util.List;

public class JpanPathSelector implements PathSelector {
    private ScionService service;

    public JpanPathSelector(ScionService service) {
        this.service = service;
    }

    @Override
    public long getLocalIsdAs() {
        return service.getLocalIsdAs();
    }

    @Override
    public RequestPath lookup(long dstIsdAs, InetAddress dstHost, int dstPort) {
        List<RequestPath> paths = service.getPaths(dstIsdAs, dstHost, dstPort);
        if (paths.isEmpty()) return null;
        return paths.get(0);
    }
}
