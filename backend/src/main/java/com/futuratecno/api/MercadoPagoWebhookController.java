package com.futuratecno.api;

import com.futuratecno.application.MercadoPagoService;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint público para Mercado Pago, protegido con la firma secreta configurada en Webhooks. */
@RestController
@RequestMapping("/api/pagos/mercadopago")
public class MercadoPagoWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final MercadoPagoService mercadoPagoService;
    private final String webhookSecret;

    public MercadoPagoWebhookController(MercadoPagoService mercadoPagoService,
                                        @Value("${mercadopago.webhook-secret:}") String webhookSecret) {
        this.mercadoPagoService = mercadoPagoService;
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "type", required = false) String type) {
        if (webhookSecret.isBlank()) {
            logger.error("Webhook de Mercado Pago recibido sin MERCADOPAGO_WEBHOOK_SECRET configurado.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Webhook no configurado."));
        }
        if (signature == null || requestId == null || dataId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Notificación incompleta."));
        }

        try {
            WebhookSignatureValidator.validate(signature, requestId, dataId, webhookSecret);
        } catch (MPInvalidWebhookSignatureException e) {
            logger.warn("Firma inválida en webhook de Mercado Pago: {}", e.getReason());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Firma inválida."));
        }

        if (type != null && !"payment".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(Map.of("recibido", true));
        }
        try {
            mercadoPagoService.procesarPago(Long.valueOf(dataId));
            return ResponseEntity.ok(Map.of("recibido", true));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Identificador inválido."));
        } catch (RuntimeException e) {
            logger.error("No se pudo procesar el webhook de Mercado Pago para payment id {}: {}", dataId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo procesar."));
        }
    }
}
