package com.futuratecno.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalHostRedirectFilterTest {

    private final CanonicalHostRedirectFilter filtro = new CanonicalHostRedirectFilter(
            "www.tecnopolisolavarria.com", "futuratecno.com.ar, www.futuratecno.com.ar");

    private MockHttpServletResponse pedir(String host, String uri, String query) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setServerName(host);
        req.setQueryString(query);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void elDominioViejoRedirigeAlNuevoConPathYQuery() throws Exception {
        MockHttpServletResponse res = pedir("www.futuratecno.com.ar", "/producto/5", "x=1");
        assertThat(res.getStatus()).isEqualTo(301);
        assertThat(res.getHeader("Location")).isEqualTo("https://www.tecnopolisolavarria.com/producto/5?x=1");
        assertThat(pedir("futuratecno.com.ar", "/", null).getHeader("Location"))
                .isEqualTo("https://www.tecnopolisolavarria.com/");
    }

    @Test
    void laApiDelDominioViejoNoSeRedirige() throws Exception {
        // El webhook de Mercado Pago hace POST a la URL vieja: un 301 lo rompería.
        assertThat(pedir("www.futuratecno.com.ar", "/api/pagos/webhook", null).getStatus()).isEqualTo(200);
    }

    @Test
    void elApexDelNuevoVaAlWwwYElCanonicoPasa() throws Exception {
        assertThat(pedir("tecnopolisolavarria.com", "/catalogo", null).getHeader("Location"))
                .isEqualTo("https://www.tecnopolisolavarria.com/catalogo");
        assertThat(pedir("www.tecnopolisolavarria.com", "/catalogo", null).getStatus()).isEqualTo(200);
        assertThat(pedir("futuratecno-production.up.railway.app", "/", null).getStatus()).isEqualTo(200);
    }
}
