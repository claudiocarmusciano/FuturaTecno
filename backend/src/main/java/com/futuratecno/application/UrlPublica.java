package com.futuratecno.application;

import java.net.InetAddress;
import java.net.URI;

/** Solo destinos HTTP públicos: evita acceder a servicios internos desde URLs aportadas por terceros. */
final class UrlPublica {
    private UrlPublica() {}
    static boolean permitida(String valor) {
        try {
            URI uri = URI.create(valor);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) return false;
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            if (host.contains("encrypted-tbn") || host.equals("localhost") || host.endsWith(".local") || host.endsWith(".internal")) return false;
            for (InetAddress ip : InetAddress.getAllByName(host)) {
                if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress() || ip.isMulticastAddress()) return false;
                byte[] b = ip.getAddress();
                if (b.length == 16 && ((b[0] & 0xfe) == 0xfc)) return false;
                if (b.length == 4 && ((b[0] & 255) == 0 || ((b[0] & 255) == 100 && (b[1] & 255) >= 64 && (b[1] & 255) <= 127) || (b[0] & 255) >= 240)) return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }
}
