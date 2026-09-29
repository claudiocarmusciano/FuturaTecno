-- Demora de entrega CONGELADA en cada renglón del pedido, como los precios: el admin puede
-- cambiar la del proveedor después, y el pedido tiene que seguir diciendo lo que el cliente
-- aceptó. NULL = la entrega normal de la tienda. Los pedidos anteriores quedan en NULL.
ALTER TABLE pedido_items ADD COLUMN demora_entrega_min_dias INTEGER;
ALTER TABLE pedido_items ADD COLUMN demora_entrega_max_dias INTEGER;
