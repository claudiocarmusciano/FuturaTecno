-- Presupuestos de "Armá tu PC" que guardan los clientes registrados (2026-11-20).
-- seleccion: lo que hace falta para reabrirlo en el armador. resumen: foto de componentes y precios
-- del momento en que se guardó, para listarlo e imprimirlo aunque los precios cambien después.
CREATE TABLE presupuestos_pc (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(120) NOT NULL,
    seleccion TEXT NOT NULL,
    resumen TEXT NOT NULL,
    total_ars NUMERIC(14,2) NOT NULL,
    total_usd NUMERIC(14,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_presupuestos_pc_usuario ON presupuestos_pc (usuario_id, updated_at DESC);
