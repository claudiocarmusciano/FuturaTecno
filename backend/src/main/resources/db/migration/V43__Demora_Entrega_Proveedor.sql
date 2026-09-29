-- Demora de entrega por proveedor, en días. NULL = la entrega normal de la tienda
-- (EtaService: 3 días hábiles). El catálogo la muestra sin nombrar al proveedor.
ALTER TABLE proveedores ADD COLUMN demora_entrega_min_dias INTEGER;
ALTER TABLE proveedores ADD COLUMN demora_entrega_max_dias INTEGER;
ALTER TABLE proveedores ADD CONSTRAINT proveedores_demora_entrega_valida CHECK (
    (demora_entrega_min_dias IS NULL AND demora_entrega_max_dias IS NULL)
    OR (demora_entrega_min_dias >= 1 AND demora_entrega_max_dias >= demora_entrega_min_dias));

-- Apple Trade trae la mercadería por encargo: 15 a 20 días (pedido del 2026-09-29).
UPDATE proveedores SET demora_entrega_min_dias = 15, demora_entrega_max_dias = 20 WHERE codigo = 'APT';
