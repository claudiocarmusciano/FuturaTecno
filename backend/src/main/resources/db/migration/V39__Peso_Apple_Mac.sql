-- "Apple > Mac" se quedó sin peso y sus 20 productos dejaron de cotizar envío. Es una consecuencia
-- directa de la V38: los iMac y los Mac mini estaban en "Notebooks > Consumo", que SÍ tiene default,
-- y al mudarlos a su árbol correcto cayeron en una subcategoría que nunca lo tuvo (la crearon desde
-- el panel, después de la V14, que es donde se cargaron los defaults a mano).
--
-- No es un detalle estético: `EnvioService` corta la cotización del CARRITO ENTERO si un solo ítem
-- no resuelve medidas, así que un Mac mini en el carrito dejaba sin cotizar todo lo demás.
-- Verificado contra producción con POST /api/envio/cotizar a CP 1425: 4 de 4 Mac daban
-- disponible=false, mientras las otras siete subcategorías de Apple cotizaban bien.
--
-- El default va en el extremo pesado, el mismo criterio de la V16: 14 de los 20 son iMac 24" y
-- quedarse corto se paga de nuestro bolsillo. Medidas de la caja de un iMac 24".
UPDATE categorias SET peso_gramos_default = 7500, alto_cm_default = 62, ancho_cm_default = 47, largo_cm_default = 18
  WHERE nombre = 'Mac' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Apple' AND padre_id IS NULL);

-- Los 6 Mac mini comparten categoría con los iMac y no se parecen en nada: cobrarles el envío de un
-- iMac es perder la venta. Van como override del producto, que es lo que manda sobre el default de
-- la categoría. Solo se rellenan los que estén en null: si alguien ya midió uno, ese dato gana.
UPDATE productos SET peso_gramos = 1300, alto_cm = 21, ancho_cm = 21, largo_cm = 9
WHERE lower(trim(marca)) = 'apple'
  AND lower(modelo) LIKE '%mac mini%'
  AND peso_gramos IS NULL AND alto_cm IS NULL AND ancho_cm IS NULL AND largo_cm IS NULL;
