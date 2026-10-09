package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Caso real del 2026-10-07: el mismo código de Elit cargado dos veces cortaba la importación entera. */
class ElitImportacionDuplicadaTest {

    @Test
    void conDosCopiasDelMismoCodigoSeQuedaConLaPrimeraEnVezDeFallar() {
        ProductoRepositoryFalso falso = new ProductoRepositoryFalso();
        Producto vieja = new Producto(); vieja.setId(11017L);
        Producto nueva = new Producto(); nueva.setId(11093L);
        falso.agregar(7L, "20736", vieja);
        falso.agregar(7L, "20736", nueva);

        assertThat(falso.repo().findByProveedorIdAndCodigoExterno(7L, "20736")).containsSame(vieja);
    }

    @Test
    void unaSegundaImportacionSimultaneaSeRechazaSinTocarNada() {
        ElitApiClient api = mock(ElitApiClient.class);
        when(api.estaConfigurado()).thenReturn(true);
        ProductoRepositoryFalso falso = new ProductoRepositoryFalso();
        falso.bloqueoLibre = false;

        ElitImportService servicio = new ElitImportService(api, falso.repo(), mock(MargenManualService.class),
                mock(VarianteRepository.class), mock(ProveedorRepository.class), mock(ImagenRepository.class),
                mock(CategoriaClasificadorService.class));

        assertThatThrownBy(servicio::sincronizar).hasMessageContaining("en curso");
        verifyNoInteractions(api);
    }
}
