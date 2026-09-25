package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static com.futuratecno.application.ImageUrlValidatorService.Verificacion.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ImageUrlValidatorServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private ImageUrlValidatorService.Verificacion con(org.springframework.test.web.client.ResponseCreator respuesta) {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo("https://93.184.215.14/foto.png")).andRespond(respuesta);
        return new ImageUrlValidatorService(rt).verificar("https://93.184.215.14/foto.png");
    }

    @Test void unaImagenRealEsImagen() {
        assertEquals(IMAGEN, con(withSuccess(PNG, MediaType.IMAGE_PNG)));
    }

    @Test void soloUn404OUn410CuentanComoMuerta() {
        assertEquals(MUERTA, con(withStatus(HttpStatus.NOT_FOUND)));
        assertEquals(MUERTA, con(withStatus(HttpStatus.GONE)));
    }

    /** Un sitio que bloquea servidores puede cargar perfecto en el navegador: no alcanza para descartar. */
    @Test void unBloqueoOUnErrorDelServidorSonDudosos() {
        assertEquals(DUDOSA, con(withStatus(HttpStatus.FORBIDDEN)));
        assertEquals(DUDOSA, con(withServerError()));
        assertEquals(DUDOSA, con(withSuccess("<html>", MediaType.TEXT_HTML)));
    }
}
