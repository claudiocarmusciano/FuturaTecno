-- Checkout Pro: trazabilidad del cobro asociada al pedido.
-- El estado comercial del pedido y el estado financiero son independientes.

ALTER TABLE pedidos ADD COLUMN estado_pago VARCHAR(20) NOT NULL DEFAULT 'SIN_INICIAR';
ALTER TABLE pedidos ADD COLUMN mercado_pago_preference_id VARCHAR(120);
ALTER TABLE pedidos ADD COLUMN mercado_pago_payment_id BIGINT;
ALTER TABLE pedidos ADD COLUMN mercado_pago_status_detail VARCHAR(150);
ALTER TABLE pedidos ADD COLUMN mercado_pago_checkout_url VARCHAR(1000);
ALTER TABLE pedidos ADD COLUMN monto_pago_ars NUMERIC(14, 2);
ALTER TABLE pedidos ADD COLUMN pagado_en TIMESTAMP;

ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado_pago CHECK (estado_pago IN (
    'SIN_INICIAR', 'PENDIENTE', 'EN_PROCESO', 'APROBADO', 'RECHAZADO',
    'CANCELADO', 'REEMBOLSADO', 'CONTRACARGO'
));

CREATE UNIQUE INDEX uq_pedidos_mp_preference
    ON pedidos (mercado_pago_preference_id)
    WHERE mercado_pago_preference_id IS NOT NULL;

CREATE UNIQUE INDEX uq_pedidos_mp_payment
    ON pedidos (mercado_pago_payment_id)
    WHERE mercado_pago_payment_id IS NOT NULL;

CREATE INDEX idx_pedidos_estado_pago ON pedidos (estado_pago);
