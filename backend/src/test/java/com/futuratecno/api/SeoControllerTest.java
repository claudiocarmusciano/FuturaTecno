package com.futuratecno.api;

import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeoControllerTest {

    private ProductoCatalogoDTO producto(String marca, String modelo, String imagen, BigDecimal... preciosArs) {
        ProductoCatalogoDTO p = new ProductoCatalogoDTO();
        p.setId(42L);
        p.setMarca(marca);
        p.setModelo(modelo);
        p.setImagenUrl(imagen);
        p.setVariantes(java.util.Arrays.stream(preciosArs)
                .map(a -> new VarianteCatalogoDTO(1L, "", BigDecimal.ONE, a)).toList());
        return p;
    }

    @Test
    void armaTituloDescripcionImagenYPrecioMinimo() {
        String b = SeoController.bloqueProducto(producto("Apple", "iPhone 17 Pro 256GB \"Silver\"",
                "https://img.test/a.png", new BigDecimal("2500000.4"), new BigDecimal("1999999.6")), "https://www.tecnopolisolavarria.com");
        assertThat(b).contains("<title>Apple iPhone 17 Pro 256GB &quot;Silver&quot; | Tecnópolis Olavarría</title>")
                .contains("<link rel=\"canonical\" href=\"https://www.tecnopolisolavarria.com/producto/42\" />")
                .contains("og:image\" content=\"https://img.test/a.png\"")
                .contains("$ 2.000.000.")
                .contains("\"price\":\"1999999.60\"")
                .contains("\"name\":\"Apple iPhone 17 Pro 256GB \\\"Silver\\\"\"");
    }

    @Test
    void noRepiteLaMarcaYSinImagenUsaLaDelSitio() {
        String b = SeoController.bloqueProducto(producto("Motorola", "Motorola G04 64GB", null), "https://www.tecnopolisolavarria.com");
        assertThat(b).contains("<title>Motorola G04 64GB | Tecnópolis Olavarría</title>")
                .contains("og-image.png")
                .doesNotContain("offers");
    }
}
