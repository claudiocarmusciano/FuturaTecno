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

/**
 * Redirige (301) el dominio apex a la URL canónica con www, preservando path y query.
 * Ej: https://futuratecno.com.ar/producto/5?x=1 → https://www.futuratecno.com.ar/producto/5?x=1
 *
 * Configurable con CANONICAL_HOST (vacío = desactivado). Solo redirige el apex exacto:
 * NO toca localhost, ni el dominio *.up.railway.app, ni el propio www.
 * Corre antes que todo (incluida la cadena de Spring Security).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CanonicalHostRedirectFilter extends OncePerRequestFilter {

    private final String canonicalHost;   // ej. www.futuratecno.com.ar
    private final String apexHost;        // canonicalHost sin el "www." inicial → futuratecno.com.ar

    public CanonicalHostRedirectFilter(@Value("${app.canonical-host:}") String canonicalHost) {
        this.canonicalHost = canonicalHost == null ? "" : canonicalHost.trim().toLowerCase();
        this.apexHost = this.canonicalHost.startsWith("www.") ? this.canonicalHost.substring(4) : "";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!apexHost.isEmpty() && apexHost.equalsIgnoreCase(req.getServerName())) {
            String qs = req.getQueryString();
            String destino = "https://" + canonicalHost + req.getRequestURI() + (qs != null ? "?" + qs : "");
            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            res.setHeader("Location", destino);
            return;
        }
        chain.doFilter(req, res);
    }
}
