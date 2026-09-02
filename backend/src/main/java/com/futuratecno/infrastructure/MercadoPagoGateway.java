package com.futuratecno.infrastructure;

import com.futuratecno.domain.Pedido;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** Único punto que conoce el SDK de Mercado Pago. El Access Token nunca sale del backend. */
@Component
public class MercadoPagoGateway {
    private final String accessToken;
    private final String publicUrl;
    private final boolean testMode;

    public MercadoPagoGateway(@Value("${mercadopago.access-token:}") String accessToken,
                              @Value("${app.public-url}") String publicUrl,
                              @Value("${mercadopago.test-mode:false}") boolean testMode) {
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.publicUrl = quitarBarraFinal(publicUrl);
        this.testMode = testMode;
    }

    public boolean estaConfigurado() {
        return !accessToken.isBlank();
    }

    public boolean esPrueba() {
        return testMode;
    }

    public PreferenciaCreada crearPreferencia(Pedido pedido, BigDecimal montoArs) {
        exigirConfiguracion();
        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(pedido.getNumero())
                .title("Pedido " + pedido.getNumero() + " - FuturaTecno")
                .description("Compra online en FuturaTecno")
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(montoArs)
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(publicUrl + "/pago/resultado?pedido=" + pedido.getNumero() + "&resultado=aprobado")
                .pending(publicUrl + "/pago/resultado?pedido=" + pedido.getNumero() + "&resultado=pendiente")
                .failure(publicUrl + "/pago/resultado?pedido=" + pedido.getNumero() + "&resultado=error")
                .build();

        OffsetDateTime vence = pedido.getVenceEn()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(PreferencePayerRequest.builder()
                        .email(pedido.getUsuario().getEmail())
                        .build())
                .externalReference(pedido.getNumero())
                .statementDescriptor("FUTURATECNO")
                .backUrls(backUrls)
                .autoReturn("approved")
                .notificationUrl(publicUrl + "/api/pagos/mercadopago/webhook")
                .expires(true)
                .expirationDateFrom(OffsetDateTime.now())
                .expirationDateTo(vence)
                .build();

        try {
            Preference preference = new PreferenceClient().create(request);
            String url = esPrueba() ? preference.getSandboxInitPoint() : preference.getInitPoint();
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("Mercado Pago no devolvió una URL de checkout.");
            }
            return new PreferenciaCreada(preference.getId(), url, esPrueba());
        } catch (MPApiException e) {
            throw new IllegalStateException("Mercado Pago rechazó la creación del checkout (HTTP "
                    + e.getStatusCode() + ").", e);
        } catch (MPException e) {
            throw new IllegalStateException("No se pudo conectar con Mercado Pago.", e);
        }
    }

    public Payment obtenerPago(Long paymentId) {
        exigirConfiguracion();
        MercadoPagoConfig.setAccessToken(accessToken);
        try {
            return new PaymentClient().get(paymentId);
        } catch (MPApiException e) {
            throw new IllegalArgumentException("Mercado Pago no encontró el pago informado.", e);
        } catch (MPException e) {
            throw new IllegalStateException("No se pudo consultar el pago en Mercado Pago.", e);
        }
    }

    private void exigirConfiguracion() {
        if (!estaConfigurado()) {
            throw new IllegalStateException("El cobro online todavía no está configurado.");
        }
    }

    private static String quitarBarraFinal(String value) {
        if (value == null || value.isBlank()) return "https://www.futuratecno.com.ar";
        String out = value.trim();
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    public record PreferenciaCreada(String id, String checkoutUrl, boolean prueba) {}
}
