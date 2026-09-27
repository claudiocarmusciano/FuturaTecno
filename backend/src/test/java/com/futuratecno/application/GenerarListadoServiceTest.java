package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GenerarListadoServiceTest {
    private static <T> T stub(Class<T> type) { return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    final ObjectMapper mapper = new ObjectMapper();
    final RestTemplate http = new RestTemplate();
    final ImagenManualService memoria = stub(ImagenManualService.class);
    final AnthropicImageService buscador = stub(AnthropicImageService.class);
    final ImageUrlValidatorService validator = stub(ImageUrlValidatorService.class);
    final GenerarListadoService service = new GenerarListadoService(http, mapper, memoria, buscador, validator);

    private void usarOpenai() {
        ReflectionTestUtils.setField(service,"proveedorIa","openai");
        ReflectionTestUtils.setField(service,"openaiKey","test-only");
    }
    @Test void generaConOpenaiSinLlamarAnthropic() throws Exception {
        usarOpenai();
        var server = MockRestServiceServer.bindTo(http).build();
        String json = "{\"articulos\":[{\"marca\":\"ASUS\",\"modelo\":\"X\",\"precio_usd\":500}],\"avisos\":[]}";
        String response = mapper.writeValueAsString(java.util.Map.of("status","completed","output",List.of(java.util.Map.of("type","message","content",List.of(java.util.Map.of("type","output_text","text",json))))));
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(header("Authorization","Bearer test-only"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.text.format.type").value("json_object"))
                .andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
        assertEquals(1, service.generar("ASUS X USD 500").articulos().size());
        server.verify(); verifyNoInteractions(buscador);
    }
    @Test void openaiSinClaveNoUsaAnthropic() {
        ReflectionTestUtils.setField(service,"proveedorIa","openai");
        assertTrue(assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage().contains("OPENAI_API_KEY"));
        verifyNoInteractions(buscador);
    }
    @Test void openaiMemoriaNoNecesitaClave() {
        ReflectionTestUtils.setField(service,"proveedorIa","openai");
        when(memoria.buscar("ASUS","X")).thenReturn(Optional.of("https://cdn.test/x.jpg"));
        assertEquals(List.of("https://cdn.test/x.jpg"),service.imagen("ASUS","X").get("imagenes"));
        verifyNoInteractions(buscador, validator);
    }
    @Test void openaiExplicaSaldoAgotadoSinReintentar() {
        usarOpenai();
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"insufficient_quota\"}}"));
        assertTrue(assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage().contains("saldo"));
        server.verify();
    }

    private void usarDeepseek() {
        ReflectionTestUtils.setField(service,"proveedorIa","deepseek");
        ReflectionTestUtils.setField(service,"deepseekKey","test-only");
    }
    @Test void generaConDeepseekSinLlamarAnthropic() throws Exception {
        usarDeepseek();
        var server = MockRestServiceServer.bindTo(http).build();
        String json = "{\"articulos\":[{\"marca\":\"ASUS\",\"modelo\":\"X\",\"precio_usd\":500}],\"avisos\":[]}";
        String response = mapper.writeValueAsString(java.util.Map.of("choices",List.of(java.util.Map.of(
                "finish_reason","stop","message",java.util.Map.of("role","assistant","content",json)))));
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(header("Authorization","Bearer test-only"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
        assertEquals(1, service.generar("ASUS X USD 500").articulos().size());
        server.verify(); verifyNoInteractions(buscador);
    }
    @Test void deepseekSinClaveNoUsaAnthropic() {
        ReflectionTestUtils.setField(service,"proveedorIa","deepseek");
        assertTrue(assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage().contains("DEEPSEEK_API_KEY"));
        verifyNoInteractions(buscador);
    }
    @Test void deepseekExplicaLimiteDeSolicitudes() {
        usarDeepseek();
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON).body("{}"));
        assertTrue(assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage().contains("límite de solicitudes"));
        server.verify();
    }
    @Test void deepseekUsaAnthropicParaBuscarImagenes() {
        usarDeepseek();
        when(memoria.buscar("ASUS","X")).thenReturn(Optional.empty());
        when(buscador.buscarImagen("ASUS X")).thenReturn(Optional.of("https://cdn.test/a.jpg"));
        when(validator.esImagenDirecta("https://cdn.test/a.jpg")).thenReturn(true);
        assertEquals(List.of("https://cdn.test/a.jpg"), service.imagen("ASUS","X").get("imagenes"));
    }
    @Test void validaFilasYEliminaCamposNoPermitidosSinAceptarImagenesInventadas() throws Exception {
        var result = service.normalizar(mapper.readTree("""
          {"articulos":[
            {"marca":"ASUS","modelo":"Vivobook 16GB 512GB","precio_usd":1250,"proveedorId":9,"imagenes":["http://inventada.test/a.jpg"],"especificaciones":{"ram":"16GB","secreto":"dato"}},
            {"marca":"","modelo":"sin marca","precio_usd":50},
            {"marca":"HP","modelo":"sin precio","precio_usd":0}
          ],"avisos":[]}
          """));
        assertEquals(1, result.articulos().size());
        var a = result.articulos().getFirst();
        assertFalse(a.containsKey("proveedorId")); assertEquals(List.of(), a.get("imagenes"));
        assertEquals(2, result.avisos().size());
        assertFalse(a.get("especificaciones").toString().contains("secreto"));
        verifyNoInteractions(memoria, buscador, validator);
    }
    @Test void unificaDenominacionAlMayorPrecioYConservaVariantes() throws Exception {
        var r = service.normalizar(mapper.readTree("""
          {"articulos":[
            {"marca":"ASUS","modelo":"X 16GB","precio_usd":500},
            {"marca":"asus","modelo":"X  16GB","precio_usd":500},
            {"marca":"ASUS","modelo":"X 16GB","precio_usd":600},
            {"marca":"ASUS","modelo":"X 32GB","precio_usd":700}
          ]}
          """));
        assertEquals(2, r.articulos().size()); assertEquals(2, r.avisos().size());
        assertEquals(new java.math.BigDecimal("600"), r.articulos().getFirst().get("precio_usd"));
    }
    @Test void precioMayorNoDependeDelOrden() throws Exception {
        var r = service.normalizar(mapper.readTree("""
          {"articulos":[
            {"marca":"ASUS","modelo":"X","precio_usd":600},
            {"marca":"ASUS","modelo":"X","precio_usd":500},
            {"marca":"ASUS","modelo":"X","precio_usd":550}
          ]}
          """));
        assertEquals(1, r.articulos().size());
        assertEquals(new java.math.BigDecimal("600"), r.articulos().getFirst().get("precio_usd"));
    }
    @Test void excluyeCondicionesEspecialesEnModeloYEspecificaciones() throws Exception {
        for (String condicion : List.of("con FALLA", "usado", "reacondicionado", "Open Box", "sin caja", "sin garantía", "pantalla rota", "de exhibición", "para repuestos")) {
            var articulo = java.util.Map.of("marca", "ASUS", "modelo", "X", "precio_usd", 500,
                "especificaciones", java.util.Map.of("otros", condicion));
            assertTrue(service.normalizar(mapper.valueToTree(java.util.Map.of("articulos", List.of(articulo)))).articulos().isEmpty(), condicion);
            var modelo = java.util.Map.of("marca", "ASUS", "modelo", "X " + condicion, "precio_usd", 500);
            assertTrue(service.normalizar(mapper.valueToTree(java.util.Map.of("articulos", List.of(modelo)))).articulos().isEmpty(), condicion);
        }
    }
    @Test void conservaOfertaNuevaYSinFallas() {
        var a = java.util.Map.of("marca", "ASUS", "modelo", "X", "precio_usd", 500,
            "especificaciones", java.util.Map.of("otros", "Nuevo en oferta, sin fallas"));
        assertEquals(1, service.normalizar(mapper.valueToTree(java.util.Map.of("articulos", List.of(a)))).articulos().size());
    }
    @Test void llamaAlProveedorYDevuelveBorradorSinPersistencia() throws Exception {
        ReflectionTestUtils.setField(service,"apiKey","test-key"); ReflectionTestUtils.setField(service,"modelo","test-model");
        var server=MockRestServiceServer.bindTo(http).build();
        String answer=mapper.writeValueAsString(java.util.Map.of("stop_reason","end_turn","content",List.of(java.util.Map.of("type","text","text","{\"articulos\":[{\"marca\":\"ASUS\",\"modelo\":\"X\",\"precio_usd\":1250}]}"))));
        server.expect(requestTo("https://api.anthropic.com/v1/messages")).andExpect(header("x-api-key","test-key"))
              .andRespond(withSuccess(answer,MediaType.APPLICATION_JSON));
        assertEquals(1,service.generar("ASUS X USD 1.250").articulos().size()); server.verify();
        verifyNoInteractions(memoria,buscador,validator);
    }
    @Test void noAceptaGeneracionTruncada() {
        ReflectionTestUtils.setField(service,"apiKey","test-key"); ReflectionTestUtils.setField(service,"modelo","test-model");
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages")).andRespond(withSuccess("{\"stop_reason\":\"max_tokens\"}",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500"));
    }
    // El caso real del 2026-09-11: Anthropic devolvió 400 por tope de uso y el admin vio
    // "revisá la conexión" con los logs vacíos. El mensaje tiene que llegar entero.
    @Test void anthropicExplicaCuotaAgotadaConElMensajeDelProveedor() {
        ReflectionTestUtils.setField(service,"apiKey","test-key"); ReflectionTestUtils.setField(service,"modelo","test-model");
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"This request would exceed your specified usage limits.\"}}"));
        String mensaje = assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage();
        assertTrue(mensaje.contains("cuota"), mensaje);
        assertTrue(mensaje.contains("specified usage limits"), mensaje);
        server.verify();
    }
    /** Un timeout tiene que decir "partí el listado", no "revisá la conexión": el proveedor está vivo. */
    @Test void explicaLaDemoraSinMandarARevisarLaConexion() {
        usarDeepseek();
        var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(req -> { throw new java.net.SocketTimeoutException("Read timed out"); });
        String mensaje = assertThrows(IllegalStateException.class, () -> service.generar("ASUS X USD 500")).getMessage();
        assertTrue(mensaje.contains("DeepSeek"), mensaje);
        assertTrue(mensaje.contains("partes más chicas"), mensaje);
    }
    /** Anthropic envuelve el JSON en ```json aunque el prompt le pida que no. Pasó en prod el 14/09. */
    @Test void aceptaElJsonEnvueltoEnMarkdown() throws Exception {
        ReflectionTestUtils.setField(service,"apiKey","test-key"); ReflectionTestUtils.setField(service,"modelo","test-model");
        var server=MockRestServiceServer.bindTo(http).build();
        String conCerco = "```json\n{\"articulos\":[{\"marca\":\"Canon\",\"modelo\":\"HFG70\",\"precio_usd\":1330}]}\n```";
        String answer = mapper.writeValueAsString(java.util.Map.of("stop_reason","end_turn",
                "content",List.of(java.util.Map.of("type","text","text",conCerco))));
        server.expect(requestTo("https://api.anthropic.com/v1/messages")).andRespond(withSuccess(answer,MediaType.APPLICATION_JSON));
        var borrador = service.generar("CANON HFG70 VIXIA UHD 4K USD 1.330");
        assertEquals(1, borrador.articulos().size());
        assertEquals("Canon", borrador.articulos().getFirst().get("marca"));
        server.verify();
    }
    @Test void elCercoDeMarkdownEsOpcional() throws Exception {
        assertEquals(1, service.leerBorrador("{\"articulos\":[1]}").path("articulos").size());
        assertEquals(1, service.leerBorrador("```json\n{\"articulos\":[1]}\n```").path("articulos").size());
        assertEquals(1, service.leerBorrador("```\n{\"articulos\":[1]}\n```").path("articulos").size());
        assertEquals(1, service.leerBorrador("  ```json\n{\"articulos\":[1]}\n```  ").path("articulos").size());
    }
    @Test void anthropicExplicaClaveRechazada() {
        ReflectionTestUtils.setField(service,"apiKey","test-key"); ReflectionTestUtils.setField(service,"modelo","test-model");
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{}"));
        assertTrue(assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500")).getMessage().contains("ANTHROPIC_API_KEY"));
        server.verify();
    }
    @Test void reutilizaMemoriaSinConsumirIaNiVerificarPorRed() {
        when(memoria.buscar("ASUS","X")).thenReturn(Optional.of("https://cdn.test/a.jpg"));
        assertEquals(List.of("https://cdn.test/a.jpg"),service.imagen("ASUS","X").get("imagenes"));
        verifyNoInteractions(buscador, validator);
    }
    @Test void guardaBusquedaValidaParaProximasCargas() {
        when(memoria.buscar("ASUS","X")).thenReturn(Optional.empty());
        when(buscador.buscarImagen("ASUS X")).thenReturn(Optional.of("https://cdn.test/a.jpg"));
        when(validator.esImagenDirecta("https://cdn.test/a.jpg")).thenReturn(true);
        service.imagen("ASUS","X");
        verify(memoria).guardarAutomatica("ASUS", "X", "https://cdn.test/a.jpg");
    }
    @Test void noGuardaImagenesInvalidas() {
        when(memoria.buscar("ASUS","X")).thenReturn(Optional.empty());
        when(buscador.buscarImagen("ASUS X")).thenReturn(Optional.of("https://cdn.test/a.jpg"));
        assertEquals(List.of(), service.imagen("ASUS","X").get("imagenes"));
        verify(memoria, never()).guardarAutomatica(anyString(), anyString(), anyString());
    }
    @Test void rechazaTextoVacioYListadosExcesivos() {
        assertThrows(IllegalArgumentException.class,()->service.generar(" "));
        assertThrows(IllegalArgumentException.class,()->service.generar("a".repeat(20001)));
        assertThrows(IllegalStateException.class,()->service.generar("ASUS X USD 500"));
    }
    @Test void bloqueaDestinosInternosYHtmlDisfrazadoDeImagen() {
        assertFalse(UrlPublica.permitida("http://127.0.0.1/test"));
        assertFalse(UrlPublica.permitida("http://169.254.169.254/latest"));
        assertFalse(UrlPublica.permitida("http://[::1]/test"));
        assertFalse(UrlPublica.permitida("file:///etc/passwd"));
        assertFalse(ImageUrlValidatorService.esFirmaImagen("<html>not an image</html>".getBytes()));
        assertTrue(ImageUrlValidatorService.esFirmaImagen(new byte[]{(byte)255,(byte)216,(byte)255,0,0,0,0,0,0,0,0,0}));
    }

    @Test
    void aceptaUnArrayDirectoYConservaColorYModeloExacto() throws Exception {
        var r = service.normalizar(mapper.readTree("""
          [{"marca":"Apple","modelo":"iPhone 17 Pro 256GB Azul","modelo_exacto":"iPhone 17 Pro 256GB Blue",
            "especificaciones":{"almacenamiento":"256GB","color":"Azul"},"precio_usd":1190,"imagenes":[]}]
          """));
        assertEquals(1, r.articulos().size());
        var a = r.articulos().get(0);
        assertEquals("iPhone 17 Pro 256GB Blue", a.get("modelo_exacto"));
        assertEquals("Azul", ((java.util.Map<?, ?>) a.get("especificaciones")).get("color"));
    }

    @Test
    void unaListaCortaVaEnUnaSolaTanda() {
        assertEquals(1, GenerarListadoService.tandas("ASUS\nVivobook 15 Core 7 150U 16GB, 512GB USD 1.020").size());
    }

    @Test
    void lasTandasNuncaSeparanUnArticuloDeSusColoresYArrastranElEncabezado() {
        StringBuilder t = new StringBuilder("RELOJES\n");
        for (int i = 1; i <= 60; i++) {
            t.append("⌚ WATCH MODELO ").append(i).append(" $").append(50 + i).append("\n");
            t.append("- GRAPHITE Black/Black METAL+BROWN LEATHER\n- ROSE Gold/Gold METAL+WHITE\n");
        }
        var partes = GenerarListadoService.tandas(t.toString());
        assertTrue(partes.size() > 1, "una lista de 60 relojes con colores tiene que partirse");
        int relojes = 0;
        for (String p : partes) {
            // Cada tanda arranca con el encabezado de sección o con un reloj, nunca con un color suelto.
            assertFalse(p.startsWith("-"), p.substring(0, 30));
            assertTrue(p.startsWith("RELOJES") || p.startsWith("⌚"), p.substring(0, 30));
            // Cada reloj lleva sus dos colores en la misma tanda.
            for (String linea : p.split("\n")) if (linea.startsWith("⌚")) relojes++;
            assertEquals(p.split("⌚").length - 1, p.split("- GRAPHITE").length - 1);
        }
        assertEquals(60, relojes, "ningún artículo se pierde ni se repite");
    }

    @Test
    void enListasConElModeloComoEncabezadoCadaTandaLoArrastra() {
        StringBuilder t = new StringBuilder();
        for (int m = 1; m <= 12; m++) {
            t.append("🔥 iPhone ").append(m).append(" Pro\n");
            for (int c = 0; c < 4; c++) t.append("* 256GB - (Black - Blue) US$").append(1000 + m * 10 + c).append("\n");
        }
        for (String p : GenerarListadoService.tandas(t.toString())) {
            assertTrue(p.startsWith("🔥 iPhone"), "la tanda empieza sin su modelo: " + p.substring(0, 20));
        }
    }

    @Test
    void alUnirLasTandasUnDuplicadoConservaElPrecioMasAlto() {
        java.util.function.BiFunction<String, String, java.util.Map<String, Object>> art = (modelo, precio) -> {
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("marca", "HP"); m.put("modelo", modelo); m.put("precio_usd", new java.math.BigDecimal(precio));
            return m;
        };
        var r = GenerarListadoService.unir(List.of(
                new GenerarListadoService.Borrador(List.of(art.apply("Victus 15 16GB 512GB", "1105")), List.of()),
                new GenerarListadoService.Borrador(List.of(art.apply("Victus 15 16GB 512GB", "1190"), art.apply("Omen 16", "1890")), List.of("x"))));
        assertEquals(2, r.articulos().size());
        assertEquals(new java.math.BigDecimal("1190"), r.articulos().get(0).get("precio_usd"));
        assertTrue(r.avisos().contains("Parte 2: x"));
    }
}
