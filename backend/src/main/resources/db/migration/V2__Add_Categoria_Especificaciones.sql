-- Categoría del producto (ej: "Notebook gamer", "Notebook reacondicionada"), detectada del título de sección.
ALTER TABLE productos ADD COLUMN categoria VARCHAR(255);

-- Especificaciones en texto libre de la variante (ej: "RYZEN 5 7535HS / RX 6550M / 8GB / 512SSD / 15.6 FHD").
-- Sirve como clave para identificar variantes al actualizar precios.
ALTER TABLE variantes ADD COLUMN especificaciones VARCHAR(500);

-- Moneda en la que el proveedor pasó el precio originalmente (USD o ARS) y el monto original,
-- para preservar la fidelidad del dato más allá del costo_usd canónico.
ALTER TABLE variantes ADD COLUMN moneda_origen VARCHAR(3);
ALTER TABLE variantes ADD COLUMN precio_origen NUMERIC(14, 2);
