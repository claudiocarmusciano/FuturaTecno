-- Los micrófonos inalámbricos ("Mic Mini 2TX + 1RX + Estuche de Carga") entraban en
-- Periféricos > Auriculares porque la IA de la carga lo sugería y la pista ganaba (2026-10-27).
-- Se mueven los que EMPIEZAN con "Mic"/"Micrófono" y están en Auriculares; nada más.
UPDATE productos
SET categoria_id = (SELECT c.id FROM categorias c JOIN categorias p ON p.id = c.padre_id
                    WHERE p.nombre = 'Periféricos' AND c.nombre = 'Micrófonos' LIMIT 1)
WHERE categoria_id = (SELECT c.id FROM categorias c JOIN categorias p ON p.id = c.padre_id
                      WHERE p.nombre = 'Periféricos' AND c.nombre = 'Auriculares' LIMIT 1)
  AND modelo ~* '^\s*(mic|mics|micr[oó]fonos?)\M'
  AND EXISTS (SELECT 1 FROM categorias c JOIN categorias p ON p.id = c.padre_id
              WHERE p.nombre = 'Periféricos' AND c.nombre = 'Micrófonos');
