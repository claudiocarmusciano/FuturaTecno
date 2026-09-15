-- La memoria de imágenes compara marca+modelo EXACTOS (salvo espacios y mayúsculas). El modelo lo
-- redacta la IA en cada carga, así que "iPhone 17 Pro 256GB eSIM", "iPhone 17 Pro 256 GB eSIM" y
-- "iPhone 17 Pro 256GB (eSIM)" son el mismo teléfono y para el código son tres artículos nuevos:
-- nace un producto nuevo, no encuentra la imagen y hay que cargarla a mano otra vez.
--
-- La clave suelta saca todo lo que no sea letra o número. Eso absorbe las diferencias de
-- redacción sin perder las que importan: 256gb ≠ 512gb, black ≠ white, "mini5pro" ≠ "mini5proplus".
-- Es una columna generada y no una vista para que se pueda indexar y mantenerse sola en cada
-- INSERT o UPDATE, sin que ningún código tenga que acordarse de actualizarla.
--
-- OJO: la misma expresión está replicada en ImagenManualService#claveSuelta (Java). Si se cambia
-- una hay que cambiar la otra, o las búsquedas dejan de encontrar en silencio. Se eligió a
-- propósito la regla más tonta posible —[^a-z0-9]— justamente para que sea fácil de replicar:
-- las tildes se caen enteras en los dos lados, que es feo pero idéntico.
-- La marca se saca del modelo cuando viene repetida ("Apple" + "Apple Pencil Pro"), porque la IA
-- a veces la repite y a veces no, y si no se normaliza acá el mismo lápiz no se encuentra a sí
-- mismo. Se hace de los dos lados —en la columna y en Java— justamente para que dé igual cuál de
-- las dos redacciones esté guardada y cuál esté entrando.
ALTER TABLE productos ADD COLUMN clave_suelta TEXT
    GENERATED ALWAYS AS (
        regexp_replace(lower(coalesce(marca, '')), '[^a-z0-9]', '', 'g')
        || regexp_replace(
               regexp_replace(lower(coalesce(modelo, '')), '[^a-z0-9]', '', 'g'),
               '^' || regexp_replace(lower(coalesce(marca, '')), '[^a-z0-9]', '', 'g'),
               '', '')
    ) STORED;

CREATE INDEX idx_productos_clave_suelta ON productos (clave_suelta);
