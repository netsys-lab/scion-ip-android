package de.ovgu.netsys.scion_ip_translator.tra;

import org.scion.jpan.RequestPath;
import org.scion.jpan.ScionService;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpanPathSelector implements PathSelector {
    private ScionService service;
    private Map<Long, RequestPath> cache;
    private boolean ignoreExpiration;

    public JpanPathSelector(ScionService service, boolean ignoreExpiration) {
        this.service = service;
        this.cache = new HashMap<Long, RequestPath>();
        this.ignoreExpiration = ignoreExpiration;
    }

    @Override
    public long getLocalIsdAs() {
        return service.getLocalIsdAs();
    }

    @Override
    public RequestPath lookup(long dstIsdAsn, InetAddress dstHost, int dstPort) {
        RequestPath path = cache.get(dstIsdAsn);
        if (path != null) {
            long now = System.currentTimeMillis();
            // FIXME: This check only works if paths are retrieved from daemon directly
            if (ignoreExpiration || (now + 1000) < (1000 * path.getExpiration()))
                return path;
        }

        List<RequestPath> paths = service.getPaths(dstIsdAsn, dstHost, dstPort);
        if (paths.isEmpty()) return null;
        path = paths.get(0);
        cache.put(dstIsdAsn, path);
        return path;
    }
}
