package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ElitSinStockTest {

    @Test
    void laSyncSacaDeLaTiendaLoQueSeQuedoSinStock() throws Exception {
        ElitApiClient api = mock(ElitApiClient.class);
        ProductoRepositoryFalso falso = new ProductoRepositoryFalso();
        ProductoRepository productos = falso.repo();
        when(api.estaConfigurado()).thenReturn(true);
        Proveedor elit = new Proveedor();
        elit.setId(7L);
        elit.setActivo(true);
        // Sin Mockito por la misma razón que ProductoRepositoryFalso.
        ProveedorRepository proveedores = (ProveedorRepository) java.lang.reflect.Proxy.newProxyInstance(
                ProveedorRepository.class.getClassLoader(), new Class<?>[]{ProveedorRepository.class},
                (proxy, m, args) -> "findByNombreIgnoreCase".equals(m.getName()) && "Elit".equals(args[0])
                        ? Optional.of(elit)
                        : m.getReturnType() == Optional.class ? Optional.empty() : null);
        when(api.consultarProductos(anyInt(), eq(1), any(), any(), any(), any())).thenReturn(new ObjectMapper().readTree(
                "{\"paginador\":{\"total\":1},\"resultado\":[{\"id\":555,\"stock_total\":0,\"precio\":10,\"iva\":21}]}"));
        Producto agotado = new Producto();
        agotado.setActivo(true);
        falso.agregar(7L, "555", agotado);

        ElitImportService servicio = new ElitImportService(api, productos, mock(MargenManualService.class),
                mock(VarianteRepository.class), proveedores, mock(ImagenRepository.class), mock(CategoriaClasificadorService.class));
        Map<String, Object> r = servicio.sincronizar();

        assertThat(agotado.getActivo()).isFalse();
        assertThat(falso.guardados).contains(agotado);
        assertThat(r.get("desactivadosSinStock")).isEqualTo(1);
    }
}
