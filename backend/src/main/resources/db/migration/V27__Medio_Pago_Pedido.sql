-- Medio elegido al confirmar. Los pedidos anteriores eran exclusivamente de Mercado Pago.
ALTER TABLE pedidos ADD COLUMN medio_pago VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO';

