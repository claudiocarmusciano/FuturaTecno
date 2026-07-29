-- Pedidos: el carrito que un cliente confirma desde el catálogo.
--
-- Los precios NO se guardan por referencia sino CONGELADOS en cada ítem. El precio de venta es
-- derivado (costo_usd * (1+flete%) * (1+margen%) * cotización) y se mueve todos los días: si el
-- pedido apuntara solo a la variante, mirar un pedido viejo mostraría precios de hoy, no los que
-- el cliente aceptó. producto_id/variante_id quedan para trazabilidad, nunca para mostrar.
--
-- vence_en: un pedido vale hasta el próximo corte de las 06:30 AR, que es cuando la sincronización
-- con los mayoristas pisa precios y stock. Pasado ese momento el scheduler lo marca VENCIDO.

CREATE SEQUENCE pedidos_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pedidos (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    total_usd NUMERIC(12, 2) NOT NULL,
    total_ars NUMERIC(14, 2) NOT NULL,
    cotizacion_usada NUMERIC(12, 2) NOT NULL,
    nombre_contacto VARCHAR(150),
    telefono_contacto VARCHAR(50),
    notas VARCHAR(1000),
    vence_en TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_pedidos_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'ENTREGADO', 'CANCELADO', 'VENCIDO'))
);

CREATE TABLE pedido_items (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT,
    variante_id BIGINT,
    -- Snapshot de lo que el cliente vio al confirmar.
    producto_nombre VARCHAR(300) NOT NULL,
    especificaciones VARCHAR(500),
    sku VARCHAR(120),
    imagen_url VARCHAR(1000),
    cantidad INTEGER NOT NULL,
    precio_unitario_usd NUMERIC(12, 2) NOT NULL,
    precio_unitario_ars NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (variante_id) REFERENCES variantes(id),
    CONSTRAINT chk_pedido_items_cantidad CHECK (cantidad > 0)
);

-- Historial del cliente y bandeja del admin.
CREATE INDEX idx_pedidos_usuario ON pedidos (usuario_id);
CREATE INDEX idx_pedidos_estado ON pedidos (estado);
-- El scheduler de vencimiento busca por estado + vence_en.
CREATE INDEX idx_pedidos_estado_vence ON pedidos (estado, vence_en);
CREATE INDEX idx_pedido_items_pedido ON pedido_items (pedido_id);
