-- Código corto por proveedor (ej. "INV", "ELI"), usado para armar un SKU camuflado por producto
-- (código + identificador del artículo) que se muestra en el catálogo público sin delatar el proveedor.
ALTER TABLE proveedores ADD COLUMN codigo VARCHAR(10);

UPDATE proveedores
SET codigo = UPPER(LEFT(REGEXP_REPLACE(nombre, '[^a-zA-Z0-9]', '', 'g'), 3))
WHERE codigo IS NULL;
