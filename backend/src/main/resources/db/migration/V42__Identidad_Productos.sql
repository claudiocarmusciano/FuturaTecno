-- Identidad del artículo, separada del nombre visible (marca + modelo).
--
-- La clave suelta de la V37 solo mira marca y modelo. En una carga por JSON el modelo lo redacta
-- la IA y las características se reparten entre el modelo y las especificaciones: "G04 4G 64GB /
-- 4GB RAM" y "Motorola G04 4G 64GB" (con ram "4GB" en las especificaciones) son el mismo teléfono
-- y la clave suelta los ve distintos. Aflojar la clave sacando RAM o capacidad fusionaría
-- variantes distintas y pisaría precios, así que la identidad se resuelve en Java
-- (IdentidadProductoService) con reglas versionadas y se guarda acá ya resuelta.
--
--   identidad_clave      clave completa, con la versión de las reglas adelante ("tel1|…", "gen1|…").
--                        NULL = todavía sin resolver (productos anteriores a la V42, duplicados
--                        históricos o casos ambiguos que esperan revisión).
--   identidad_version    versión de las reglas que produjo la clave ("telefono-v1", "generico-v1").
--   identidad_familia    marca + modelo base, sin capacidad ni color: agrupa candidatos para
--                        detectar conflictos. No es única.
--   identidad_atributos  JSON con lo que se extrajo y de dónde, para auditar la decisión.
--
-- La clave suelta se conserva: la siguen usando la memoria de imágenes y la búsqueda de candidatos.
ALTER TABLE productos
    ADD COLUMN identidad_clave     TEXT,
    ADD COLUMN identidad_version   VARCHAR(30),
    ADD COLUMN identidad_familia   TEXT,
    ADD COLUMN identidad_atributos TEXT;

-- Unicidad por proveedor + identidad, INCLUYENDO inactivos: una recarga tiene que reactivar el
-- mismo producto, no crear otro al lado del dado de baja. Es parcial (WHERE … IS NOT NULL) a
-- propósito: los duplicados históricos quedan sin identidad hasta que el admin los revise
-- (GET /api/admin/identidad/transicion), así que esta migración no puede fallar por ellos.
-- Elit e Invid siguen deduplicando por codigo_externo y no se tocan.
CREATE UNIQUE INDEX ux_productos_proveedor_identidad
    ON productos (proveedor_id, identidad_clave)
    WHERE identidad_clave IS NOT NULL;

CREATE INDEX idx_productos_proveedor_familia
    ON productos (proveedor_id, identidad_familia)
    WHERE identidad_familia IS NOT NULL;

-- La memoria por marca+modelo se busca también por la identidad de otros productos (el mismo
-- artículo redactado distinto en otro proveedor): este índice la sostiene.
CREATE INDEX idx_productos_identidad_clave ON productos (identidad_clave)
    WHERE identidad_clave IS NOT NULL;
