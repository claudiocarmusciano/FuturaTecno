-- El flete pasa de ser un monto fijo en USD a un porcentaje del valor del producto
-- (similar al margen). Se renombra la columna; el valor existente queda interpretado como %.
ALTER TABLE proveedores RENAME COLUMN costo_flete_usd TO flete_porcentaje;
