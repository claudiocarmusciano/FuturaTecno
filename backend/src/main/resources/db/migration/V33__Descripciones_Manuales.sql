-- Memoria de descripciones curadas a mano, espejo de imagenes_manuales (V31).
-- La "descripción" de un producto es variantes.especificaciones: no hay columna aparte.
-- Clave marca+modelo normalizados, igual que las imágenes, para que la descripción sobreviva
-- a un reimport y se comparta entre proveedores.
CREATE TABLE descripciones_manuales (
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    PRIMARY KEY (marca, modelo)
);

-- Recupera lo que ya está cargado a mano para no arrancar con la memoria vacía.
-- Se excluyen ELIT e INVID a propósito: esos mayoristas reescriben su ficha en cada
-- sincronización, así que recordarla no aporta nada y taparía las descripciones propias.
INSERT INTO descripciones_manuales (marca, modelo, descripcion)
SELECT DISTINCT ON (lower(regexp_replace(trim(p.marca), '\s+', ' ', 'g')),
                    lower(regexp_replace(trim(p.modelo), '\s+', ' ', 'g')))
       lower(regexp_replace(trim(p.marca), '\s+', ' ', 'g')),
       lower(regexp_replace(trim(p.modelo), '\s+', ' ', 'g')),
       v.especificaciones
FROM productos p
JOIN variantes v ON v.producto_id = p.id
WHERE coalesce(upper(trim(p.fuente)), '') NOT IN ('ELIT', 'INVID')
  AND nullif(trim(p.marca), '') IS NOT NULL
  AND nullif(trim(p.modelo), '') IS NOT NULL
  AND nullif(trim(v.especificaciones), '') IS NOT NULL
ORDER BY lower(regexp_replace(trim(p.marca), '\s+', ' ', 'g')),
         lower(regexp_replace(trim(p.modelo), '\s+', ' ', 'g')),
         v.id;
