package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Con Jev configurado, el paso de IA usa solo Jev y descarta lo que elige con poca confianza. */
class CategoriaClasificadorJevTest {

    private final CategoriaService categorias = mock(CategoriaService.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final ClasificadorPorNombre porNombre = mock(ClasificadorPorNombre.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final JevClient jev = mock(JevClient.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final RestTemplate claude = mock(RestTemplate.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final CategoriaClasificadorService servicio = new CategoriaClasificadorService(claude, categorias, porNombre, jev);

    private final Producto producto = new Producto();

    CategoriaClasificadorJevTest() {
        producto.setMarca("Genérica");
        producto.setModelo("Aro de luz LED 26cm con trípode");
        ReflectionTestUtils.setField(servicio, "apiKey", "sk-ant-prueba");
        ReflectionTestUtils.setField(servicio, "confianzaMinimaJev", 0.6);
        when(jev.configurado()).thenReturn(true);
        when(categorias.pathsDeHoja()).thenReturn(List.of("DESTACADOS", "ACCESORIOS", "PERIFÉRICOS > WEB CAM"));
        // Sin esto Mockito devuelve 0L y el clasificador "resuelve" antes de llegar a la IA.
        when(categorias.idPorPath(any())).thenReturn(null);
        when(categorias.idPorPath("ACCESORIOS")).thenReturn(1L);
    }

    @Test
    void usaLaCategoriaDeJevCuandoEstaSeguro() {
        when(jev.elegir(any(), anyString(), anyList())).thenReturn(new JevClient.Eleccion("ACCESORIOS", 0.85));

        assertEquals(1L, servicio.clasificar(producto, null));
        verifyNoInteractions(claude);
    }

    @Test
    void anteLaDudaDejaNullYNoLePreguntaAClaude() {
        when(jev.elegir(any(), anyString(), anyList())).thenReturn(new JevClient.Eleccion("ACCESORIOS", 0.4));

        assertNull(servicio.clasificar(producto, null));
        verifyNoInteractions(claude);
    }

    @Test
    void destacadosNoSeOfreceComoOpcion() {
        servicio.clasificar(producto, null);

        verify(jev).elegir(any(), anyString(), eq(List.of("ACCESORIOS", "PERIFÉRICOS > WEB CAM")));
    }
}
