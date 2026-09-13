-- Memoria de categoría y medidas por marca+modelo, tercera de la familia junto a
-- imagenes_manuales (V31) y descripciones_manuales (V33).
--
-- Hasta ahora estos dos datos vivían solo en la fila del producto, que se busca por
-- proveedor+marca+modelo: al cargar el mismo artículo con OTRO proveedor nacía una fila nueva
-- con categoria_id y medidas en null. Eso duele especialmente porque un producto sin categoría
-- no resuelve el peso, y EnvioService corta la cotización del carrito entero si un solo ítem
-- no tiene medidas.
--
-- A diferencia de las descripciones, acá NO se excluye a Elit ni Invid: ningún mayorista trae
-- peso ni dimensiones, y la mayor parte del trabajo de categorización es justamente sobre su
-- catálogo. Una medida real de un producto es cierta venga de donde venga.
CREATE TABLE atributos_manuales (
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    categoria_id BIGINT REFERENCES categorias(id) ON DELETE SET NULL,
    peso_gramos INTEGER,
    alto_cm INTEGER,
    ancho_cm INTEGER,
    largo_cm INTEGER,
    PRIMARY KEY (marca, modelo)
);

-- Siembra con lo que ya está cargado. Ante varias filas del mismo marca+modelo (distintos
-- proveedores) gana la más completa: primero la que tiene las cuatro medidas, después la que
-- tiene categoría, y a igualdad la más reciente.
INSERT INTO atributos_manuales (marca, modelo, categoria_id, peso_gramos, alto_cm, ancho_cm, largo_cm)
SELECT DISTINCT ON (m, mo) m, mo, categoria_id, peso_gramos, alto_cm, ancho_cm, largo_cm
FROM (
    SELECT lower(regexp_replace(trim(marca), '\s+', ' ', 'g')) AS m,
           lower(regexp_replace(trim(modelo), '\s+', ' ', 'g')) AS mo,
           categoria_id, peso_gramos, alto_cm, ancho_cm, largo_cm, id
    FROM productos
    WHERE nullif(trim(marca), '') IS NOT NULL
      AND nullif(trim(modelo), '') IS NOT NULL
      AND (categoria_id IS NOT NULL OR peso_gramos IS NOT NULL OR alto_cm IS NOT NULL
           OR ancho_cm IS NOT NULL OR largo_cm IS NOT NULL)
) candidatos
ORDER BY m, mo,
         (peso_gramos IS NOT NULL AND alto_cm IS NOT NULL
          AND ancho_cm IS NOT NULL AND largo_cm IS NOT NULL) DESC,
         (categoria_id IS NOT NULL) DESC,
         id DESC;
