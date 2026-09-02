CREATE TABLE promociones (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(160),
    texto VARCHAR(500),
    enlace VARCHAR(500),
    orden INTEGER NOT NULL DEFAULT 0,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    imagen_escritorio BYTEA NOT NULL,
    mime_escritorio VARCHAR(30) NOT NULL,
    imagen_movil BYTEA,
    mime_movil VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_promociones_orden CHECK (orden >= 0),
    CONSTRAINT ck_promociones_fechas CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin > fecha_inicio)
);

CREATE INDEX idx_promociones_publicacion ON promociones (activo, orden, fecha_inicio, fecha_fin);

