-- Las URLs de fotos de Apple (store.storeimages.cdn-apple.com) traen un token ".v=" largo y pasan
-- los 500 caracteres: la carga JSON fallaba entera con "value too long" (2026-10-27).
-- El trigger de la V32 depende de productos.imagen_url y Postgres no deja cambiarle el tipo con el
-- trigger puesto: se saca y se vuelve a crear igual.
DROP TRIGGER recordar_imagen_producto ON productos;
ALTER TABLE imagenes ALTER COLUMN url TYPE VARCHAR(2000);
ALTER TABLE productos ALTER COLUMN imagen_url TYPE VARCHAR(2000);
ALTER TABLE pedido_items ALTER COLUMN imagen_url TYPE VARCHAR(2000);
CREATE TRIGGER recordar_imagen_producto
AFTER INSERT OR UPDATE OF imagen_url ON productos
FOR EACH ROW EXECUTE FUNCTION recordar_imagen_producto();
