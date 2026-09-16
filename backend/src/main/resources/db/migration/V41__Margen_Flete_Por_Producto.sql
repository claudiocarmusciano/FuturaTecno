-- El margen y el flete eran por PROVEEDOR: todo lo que entra por Elit lleva el mismo margen. Esto
-- permite pisarlos en un producto puntual —un artículo de reventa, una oferta, algo que no tolera
-- el margen general— sin tocar el resto del catálogo.
--
-- Mismo patrón que el peso y las dimensiones (V14): columnas OPCIONALES. En null significa
-- "usá el valor del proveedor", no 0%. Esa distinción importa: 0% de margen sería vender al costo.
ALTER TABLE productos ADD COLUMN margen_porcentaje NUMERIC(6, 2);
ALTER TABLE productos ADD COLUMN flete_porcentaje  NUMERIC(6, 2);

-- Memoria del override, para que sobreviva a que la fila del producto se vuelva a crear (por
-- ejemplo si el mayorista le cambia el código interno y nace como producto nuevo).
--
-- A diferencia de las otras memorias (imágenes, descripciones, atributos), esta lleva el
-- proveedor en la clave. Es deliberado: la imagen de un iPhone 15 es la misma venga de donde
-- venga, pero el margen es una decisión comercial atada a lo que ESE mayorista te cobra. Cruzar
-- el margen de Elit al mismo modelo comprado en Invid daría un precio equivocado.
CREATE TABLE margenes_manuales (
    proveedor_id BIGINT NOT NULL REFERENCES proveedores(id) ON DELETE CASCADE,
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    margen_porcentaje NUMERIC(6, 2),
    flete_porcentaje  NUMERIC(6, 2),
    PRIMARY KEY (proveedor_id, marca, modelo)
);
