-- Usuarios del sistema: ADMIN (gestiona el panel) y USUARIO (clientes registrados).
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(255),
    rol VARCHAR(20) NOT NULL DEFAULT 'USUARIO',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
