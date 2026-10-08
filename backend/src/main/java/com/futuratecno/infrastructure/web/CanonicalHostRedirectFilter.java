package com.futuratecno.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redirige (301) a la URL canónica con www, preservando path y query:
 * <ul>
 *   <li>el apex del dominio canónico (CANONICAL_HOST, ej. tecnopolisolavarria.com → www.…);</li>
 *   <li>los dominios viejos de REDIRECT_HOSTS (lista separada por comas, ej. el de Futura Tecno).</li>
 * </ul>
 * Los dominios viejos NO se redirigen en {@code /api/**}: el webhook de Mercado Pago y cualquier
 * integración configurada con la URL anterior hacen POST, y un 301 los rompería (los clientes HTTP
 * no reenvían el cuerpo). Las páginas sí se redirigen, que es lo que ven las personas y Google.
 *
 * CANONICAL_HOST vacío = desactivado. No toca localhost ni el dominio *.up.railway.app.
 * Corre antes que todo (incluida la cadena de Spring Security).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CanonicalHostRedirectFilter extends OncePerRequestFilter {

    private final String canonicalHost;      // ej. www.tecnopolisolavarria.com
    private final String apexHost;           // canonicalHost sin el "www." inicial
    private final Set<String> hostsViejos;   // ej. futuratecno.com.ar, www.futuratecno.com.ar

    public CanonicalHostRedirectFilter(@Value("${app.canonical-host:}") String canonicalHost,
                                       @Value("${app.redirect-hosts:}") String redirectHosts) {
        this.canonicalHost = canonicalHost == null ? "" : canonicalHost.trim().toLowerCase();
        this.apexHost = this.canonicalHost.startsWith("www.") ? this.canonicalHost.substring(4) : "";
        this.hostsViejos = redirectHosts == null ? Set.of() : Arrays.stream(redirectHosts.split(","))
                .map(String::trim).map(String::toLowerCase).filter(h -> !h.isEmpty())
                .filter(h -> !h.equals(this.canonicalHost))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!canonicalHost.isEmpty()) {
            String host = req.getServerName() == null ? "" : req.getServerName().toLowerCase();
            boolean esApex = !apexHost.isEmpty() && apexHost.equals(host);
            boolean esViejo = hostsViejos.contains(host) && !req.getRequestURI().startsWith("/api/");
            if (esApex || esViejo) {
                String qs = req.getQueryString();
                String destino = "https://" + canonicalHost + req.getRequestURI() + (qs != null ? "?" + qs : "");
                res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                res.setHeader("Location", destino);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
