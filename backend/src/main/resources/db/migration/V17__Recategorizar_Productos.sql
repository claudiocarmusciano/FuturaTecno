-- Recategorización de productos que quedaron colgando de la categoría equivocada.
--
-- Salieron de la revisión de los defaults de peso/dimensiones de la V16: al mirar qué productos
-- heredaban qué default aparecieron siete que directamente no pertenecen al rubro donde están.
-- La V16 les puso el peso real por override para frenar la pérdida en el envío, pero no los movió
-- de categoría a propósito: mover un producto cambia dónde lo ve el cliente en el catálogo y es una
-- decisión de negocio, no un arreglo de datos. Confirmada por el dueño del negocio el 2026-07-31,
-- después de verificar contra el catálogo de producción que los siete están mal clasificados.
--
-- Los overrides de peso que sembró la V16 NO se tocan y siguen siendo correctos: el override del
-- producto le gana al default de la categoría, así que el bulto no cambia porque cambie el rubro.
--
-- Esto sobrevive a la sincronización diaria: Elit/Invid solo clasifican cuando categoria_id viene
-- en null (ver InvidImportService, `if (producto.getCategoriaId() == null)`). La sync sí pisa el
-- campo de texto `productos.categoria` con el rubro crudo del mayorista, pero eso es solo el insumo
-- del clasificador — el catálogo muestra el nombre que resuelve categoria_id contra el árbol
-- (CatalogoService → CategoriaService.resolverNombres), así que alcanza con mover el id.
--
-- Se matchea por (proveedor.codigo, codigo_externo), la misma clave de dedup de la importación, y
-- la categoría destino por nombre + nombre del padre, no por id: las hojas 'Sillas' y 'Escritorios'
-- no existen en la V10, las creó después el ABM de categorías, así que sus ids no son estables
-- entre entornos. Si una hoja destino no existiera, la fila simplemente no matchea y no rompe nada.

WITH destinos(proveedor_codigo, codigo_externo, categoria, categoria_padre) AS (
    VALUES
    -- Los dos que estaban en 'Notebooks > Consumo' sin ser notebooks. El gemelo de la silla
    -- (INV-0417335, la Tela Gris) no se toca: está en DESTACADOS, que es una vidriera y no un rubro,
    -- así que ahí está a propósito. Van a quedar en categorías distintas mientras dure el destaque.
    ('INV', '0417336', 'Sillas',  'Sillas y escritorios'),  -- Silla Gamer Raptor Throne R1 Tela Negra
    ('INV', '0415685', 'Ink Jet', 'Impresoras'),            -- Epson EcoTank L8050

    -- Cinco access points TP-Link cargados como switches. Es el error espejo del que arregló la V16:
    -- la hoja 'Switches Administrables' quedó dimensionada para un SG3452P de 48 puertos (7 kg de
    -- rack 1U) y un AP de cielorraso pesa ~1,6 kg, así que además de aparecer en el rubro equivocado
    -- iban a cotizar el envío de más apenas se aplique la V16.
    ('INV', '0416843', 'Access Point y Extensores de Rango', 'Conectividad'),  -- EAP650-Outdoor AX3000 Wi-Fi 6
    ('INV', '0418361', 'Access Point y Extensores de Rango', 'Conectividad'),  -- EAP723 BE5000 Ceiling Mount Wi-Fi 7
    ('INV', '0418362', 'Access Point y Extensores de Rango', 'Conectividad'),  -- EAP725-Outdoor BE5000 Wi-Fi 7
    ('INV', '0418411', 'Access Point y Extensores de Rango', 'Conectividad'),  -- EAP215-Bridge KIT enlace exterior 5 km
    ('INV', '0418412', 'Access Point y Extensores de Rango', 'Conectividad')   -- EAP725-Wall BE3600 Wi-Fi 7 2.5G
)
UPDATE productos p
SET categoria_id = hoja.id,
    updated_at   = now()
FROM destinos d
JOIN proveedores pv ON pv.codigo = d.proveedor_codigo
JOIN categorias padre ON padre.nombre = d.categoria_padre AND padre.padre_id IS NULL
JOIN categorias hoja  ON hoja.nombre = d.categoria AND hoja.padre_id = padre.id
WHERE p.proveedor_id = pv.id
  AND p.codigo_externo = d.codigo_externo;
