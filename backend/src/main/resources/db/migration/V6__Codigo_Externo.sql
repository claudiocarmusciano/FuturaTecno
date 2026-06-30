-- Soporte para integraciones con distribuidores (Elit, Invid, etc.):
-- guardamos el código del producto en el sistema externo para deduplicar y sincronizar.
ALTER TABLE productos ADD COLUMN codigo_externo VARCHAR(100);
ALTER TABLE productos ADD COLUMN fuente VARCHAR(50);

CREATE INDEX idx_productos_codigo_externo ON productos(proveedor_id, codigo_externo);
