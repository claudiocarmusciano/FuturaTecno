-- Apple tiene árbol propio (iPhone, iPad, Mac, MacBook, Watch, AirPods, Monitores, Accesorios),
-- pero el clasificador lo ignoraba y repartía sus productos por el árbol genérico: los iPhone en
-- "Celulares", los MacBook en "Notebooks > Consumo", los iMac y Mac mini TAMBIÉN ahí (y ni
-- siquiera son notebooks), el Magic Mouse en "Periféricos > Mouse". Al 2026-09-15 eran 107 de 263.
--
-- Esta migración acomoda lo ya cargado; `ClasificadorPorNombre#resolverApple` evita que vuelva a
-- pasar con las cargas nuevas. Las reglas de acá replican las de allá, en el mismo orden.
--
-- El orden es al revés de lo intuitivo: los accesorios van PRIMERO porque casi todos nombran al
-- aparato que acompañan ("Magic Keyboard for iPad" es un accesorio, no un iPad). Cada UPDATE
-- excluye a los ya reubicados, así el primero que matchea gana, igual que en Java.
--
-- Las categorías se resuelven POR NOMBRE, no por id: los ids de este árbol se crearon a mano en
-- distintas migraciones y no hay garantía de que coincidan en otra base.

CREATE TEMP TABLE apple_destino (patron TEXT, hoja TEXT, orden INT);
INSERT INTO apple_destino VALUES
    ('keyboard|teclado|mouse|trackpad|folio|smart cover|apple pencil|pencil|air ?tag|funda|case|carcasa|correa|strap|cable|adaptador|cargador|soporte|dock', 'Accesorios', 1),
    ('air ?pods?|ear ?pods?', 'AirPods',   2),
    ('watch',                 'Watch',     3),
    ('mac ?book',             'MacBook',   4),
    ('imac|mac ?mini|mac ?studio|mac ?pro|mac', 'Mac', 5),
    ('iphone',                'iPhone',    6),
    ('ipad',                  'iPad',      7),
    ('display|monitor',       'Monitores', 8);

WITH apple AS (
    SELECT c.id, c.nombre
    FROM categorias c JOIN categorias p ON p.id = c.padre_id
    WHERE lower(p.nombre) = 'apple'
),
-- Igual que en Java, el tipo se decide por las primeras 4 palabras del modelo: el resto son
-- especificaciones que mienten ("Funda para MacBook" no es una MacBook).
objetivo AS (
    SELECT DISTINCT ON (pr.id) pr.id AS producto_id, apple.id AS categoria_id
    FROM productos pr
    CROSS JOIN apple_destino d
    JOIN apple ON apple.nombre = d.hoja
    WHERE lower(trim(pr.marca)) = 'apple'
      AND array_to_string((string_to_array(lower(trim(pr.modelo)), ' '))[1:4], ' ') ~ d.patron
    ORDER BY pr.id, d.orden
)
UPDATE productos pr
SET categoria_id = o.categoria_id
FROM objetivo o
WHERE pr.id = o.producto_id
  AND pr.categoria_id IS DISTINCT FROM o.categoria_id;

-- Lo que quedó sin matchear va a Accesorios, que es lo que hace el default del clasificador.
-- Un producto sin categoría no resuelve el peso, y EnvioService corta la cotización del carrito
-- entero si un solo ítem no tiene medidas: dejarlo en null sería peor que ponerlo acá.
UPDATE productos pr
SET categoria_id = (SELECT c.id FROM categorias c JOIN categorias p ON p.id = c.padre_id
                    WHERE lower(p.nombre) = 'apple' AND c.nombre = 'Accesorios' LIMIT 1)
WHERE lower(trim(pr.marca)) = 'apple'
  AND (pr.categoria_id IS NULL
       OR pr.categoria_id NOT IN (SELECT c.id FROM categorias c JOIN categorias p ON p.id = c.padre_id
                                  WHERE lower(p.nombre) = 'apple'));

DROP TABLE apple_destino;
