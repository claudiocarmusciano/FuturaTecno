package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** La pista de categoría de una carga ("Celulares") no puede sacar a Apple de su árbol. */
class CategoriaClasificadorAppleTest {

    private final CategoriaService categorias = mock(CategoriaService.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final ClasificadorPorNombre porNombre = mock(ClasificadorPorNombre.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final CategoriaClasificadorService servicio = new CategoriaClasificadorService(new RestTemplate(), categorias, porNombre);

    private static Producto producto(String marca, String modelo) {
        Producto p = new Producto();
        p.setMarca(marca);
        p.setModelo(modelo);
        return p;
    }

    @Test
    void appleVaASuArbolAunqueLaCargaSugieraCelulares() {
        Producto ipad = producto("Apple", "iPad Pro M4 13 2TB Nano Texture");
        when(porNombre.clasificar(ipad)).thenReturn("Apple > iPad");
        when(categorias.idPorPath("Apple > iPad")).thenReturn(96L);
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        assertEquals(96L, servicio.clasificar(ipad, "Celulares"));
    }

    @Test
    void otraMarcaSigueUsandoLaPista() {
        Producto galaxy = producto("Samsung", "Galaxy S26 Ultra 12/256GB");
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        assertEquals(107L, servicio.clasificar(galaxy, "Celulares"));
        verifyNoInteractions(porNombre);
    }
}
