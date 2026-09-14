-- Un producto cargado por JSON tiene UNA sola variante: la capacidad, el color y la versión
-- SIM/eSIM viajan dentro del modelo ("iPhone 17 Pro 256GB eSIM"), así que las especificaciones
-- son una descripción, no algo que distinga. Pero CargaJsonService buscaba la variante por el
-- TEXTO EXACTO de esas especificaciones, y ese texto lo redacta la IA en cada carga: "256GB ·
-- Versión eSIM" y "256GB · eSIM" son el mismo teléfono y terminaron siendo dos filas.
--
-- El daño no era estético. Las dos variantes quedaban con precios distintos, el cliente veía
-- las dos y elegía la barata — la vieja —, y como el precio del pedido se congela al confirmar,
-- se vendía por debajo del costo de hoy: US$ 111 de diferencia en el iPhone 17 Pro 512GB,
-- US$ 98 en el iPhone 17 256GB SIM. Al 2026-09-13 había 23 productos así (20 de COZ, 3 de KAD).
--
-- Sobrevive la variante con `updated_at` más reciente: cada carga redacta un solo texto y ese
-- texto matcheaba a lo sumo una fila, así que la última tocada es la del precio vigente. El id
-- más alto queda como desempate. La descripción que se conserva es la más larga del grupo,
-- porque alguna carga nueva la dejó vacía (el Apple Pencil pasó de "Stylus para iPad con carga
-- USB-C" a "").
--
-- Soft delete, como en todo el proyecto: activo=false, nunca DELETE — pedido_items apunta acá
-- por trazabilidad y un borrado físico rompería pedidos ya confirmados.
-- Se excluye a ELIT e INVID: sus productos sí pueden tener variantes reales, y los importa
-- otro código que esta migración no toca.

-- 1) La variante que sobrevive se queda con la mejor descripción del grupo. Va primero, mientras
--    todas siguen activas y el texto bueno todavía es visible.
WITH duplicadas AS (
    SELECT v.id,
           v.producto_id,
           v.especificaciones,
           first_value(v.id) OVER (PARTITION BY v.producto_id
                                   ORDER BY v.updated_at DESC, v.id DESC) AS ganadora,
           count(*)          OVER (PARTITION BY v.producto_id)            AS total
    FROM variantes v
    JOIN productos p ON p.id = v.producto_id
    WHERE v.activo = true
      AND coalesce(upper(trim(p.fuente)), '') NOT IN ('ELIT', 'INVID')
),
mejor_texto AS (
    SELECT DISTINCT ON (producto_id) producto_id, ganadora, especificaciones
    FROM duplicadas
    WHERE total > 1
      AND nullif(trim(especificaciones), '') IS NOT NULL
    ORDER BY producto_id, length(especificaciones) DESC, id
)
UPDATE variantes v
SET especificaciones = m.especificaciones
FROM mejor_texto m
WHERE v.id = m.ganadora
  AND v.especificaciones IS DISTINCT FROM m.especificaciones;

-- 2) Las sobrantes se dan de baja.
WITH duplicadas AS (
    SELECT v.id,
           v.producto_id,
           first_value(v.id) OVER (PARTITION BY v.producto_id
                                   ORDER BY v.updated_at DESC, v.id DESC) AS ganadora,
           count(*)          OVER (PARTITION BY v.producto_id)            AS total
    FROM variantes v
    JOIN productos p ON p.id = v.producto_id
    WHERE v.activo = true
      AND coalesce(upper(trim(p.fuente)), '') NOT IN ('ELIT', 'INVID')
)
UPDATE variantes v
SET activo = false
FROM duplicadas d
WHERE v.id = d.id
  AND d.total > 1
  AND d.id <> d.ganadora;
