-- IVA que se suma a los productos al cobrar el pedido (transferencia y Mercado Pago van facturados).
-- Los pedidos existentes quedan en 0 para que su total no cambie; efectivo siempre va en 0.
ALTER TABLE pedidos ADD COLUMN iva_porcentaje numeric(5,2) NOT NULL DEFAULT 0;
