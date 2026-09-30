-- Órdenes de venta que el admin carga a mano (cliente que no compra por la web).
-- Pueden no tener cuenta de usuario (quedan los datos de contacto) y no vencen a las 06:30:
-- el precio lo acordó una persona, no el catálogo del día.
ALTER TABLE pedidos ALTER COLUMN usuario_id DROP NOT NULL;
ALTER TABLE pedidos ALTER COLUMN vence_en DROP NOT NULL;
ALTER TABLE pedidos ADD COLUMN origen VARCHAR(10) NOT NULL DEFAULT 'WEB';
ALTER TABLE pedidos ADD COLUMN email_contacto VARCHAR(190);
ALTER TABLE pedidos ADD CONSTRAINT pedidos_origen_valido CHECK (origen IN ('WEB', 'MANUAL'));
ALTER TABLE pedidos ADD CONSTRAINT pedidos_web_con_usuario CHECK (origen = 'MANUAL' OR usuario_id IS NOT NULL);

-- Precio del catálogo al momento de la orden, solo cuando el admin lo cambió a mano: deja
-- registro de cuánto se modificó. NULL = se vendió al precio del catálogo.
ALTER TABLE pedido_items ADD COLUMN precio_catalogo_usd NUMERIC(19,2);
