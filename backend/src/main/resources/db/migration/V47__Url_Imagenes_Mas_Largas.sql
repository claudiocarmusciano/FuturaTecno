-- Las URLs de fotos de Apple (store.storeimages.cdn-apple.com) traen un token ".v=" largo y pasan
-- los 500 caracteres: la carga JSON fallaba entera con "value too long" (2026-10-27).
ALTER TABLE imagenes ALTER COLUMN url TYPE VARCHAR(2000);
ALTER TABLE productos ALTER COLUMN imagen_url TYPE VARCHAR(2000);
ALTER TABLE pedido_items ALTER COLUMN imagen_url TYPE VARCHAR(2000);
