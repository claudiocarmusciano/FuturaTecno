-- La tabla "imagenes" se creó en la V1 apuntando a variantes pero nunca se usó (0 filas).
-- Se repunta a productos, que es donde vive la imagen principal (imagen_url) y donde tiene
-- sentido el concepto de galería (una imagen se comparte entre variantes de un mismo producto).
-- DROP COLUMN arrastra automáticamente la FK y cualquier índice sobre variante_id, sin
-- necesidad de conocer el nombre exacto de esas constraints.
ALTER TABLE imagenes DROP COLUMN variante_id;
ALTER TABLE imagenes ADD COLUMN producto_id BIGINT NOT NULL REFERENCES productos(id);

CREATE INDEX idx_imagenes_producto ON imagenes(producto_id);
