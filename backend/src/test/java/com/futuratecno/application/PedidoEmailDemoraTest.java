package com.futuratecno.application;

import com.futuratecno.domain.Pedido;
import com.futuratecno.domain.PedidoItem;
import com.futuratecno.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PedidoEmailDemoraTest {

    @Test
    void elMailDelClienteAvisaLaDemoraSoloEnElArticuloQueLaTiene() {
        EmailService email = mock(EmailService.class);
        Pedido pedido = new Pedido();
        Usuario u = new Usuario();
        u.setEmail("cliente@example.com");
        pedido.setUsuario(u);
        pedido.setNumero("FT-1");
        pedido.setTotalUsd(new BigDecimal("1500"));
        pedido.setTotalArs(new BigDecimal("1500000"));
        pedido.setVenceEn(LocalDateTime.of(2026, 9, 30, 6, 30));
        pedido.getItems().add(item("iPhone 17 Pro 256GB", 15, 20));
        pedido.getItems().add(item("Mouse Logitech", null, null));

        new PedidoEmailService(email).notificarPedidoNuevo(pedido);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(email).enviarHtmlAsync(eq("cliente@example.com"), anyString(), html.capture());
        assertTrue(html.getValue().contains("iPhone 17 Pro 256GB<br><span style=\"color:#5D6B14;font-size:12px;font-weight:bold\">Entrega en 15 a 20 días"));
        assertEquals(1, html.getValue().split("Entrega en", -1).length - 1);
    }

    private static PedidoItem item(String nombre, Integer min, Integer max) {
        PedidoItem i = new PedidoItem();
        i.setProductoNombre(nombre);
        i.setCantidad(1);
        i.setPrecioUnitarioUsd(new BigDecimal("100"));
        i.setPrecioUnitarioArs(new BigDecimal("100000"));
        i.setDemoraEntregaMinDias(min);
        i.setDemoraEntregaMaxDias(max);
        return i;
    }
}
