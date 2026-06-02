-- Create proveedores table
CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    margen_porcentaje NUMERIC(5, 2) NOT NULL,
    costo_flete_usd NUMERIC(10, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create productos table
CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT NOT NULL,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

-- Create variantes table
CREATE TABLE variantes (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    almacenamiento_gb INTEGER,
    ram_gb INTEGER,
    color VARCHAR(100),
    costo_usd NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Create imagenes table
CREATE TABLE imagenes (
    id BIGSERIAL PRIMARY KEY,
    variante_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (variante_id) REFERENCES variantes(id)
);

-- Create indexes
CREATE INDEX idx_productos_proveedor ON productos(proveedor_id);
CREATE INDEX idx_variantes_producto ON variantes(producto_id);
CREATE INDEX idx_imagenes_variante ON imagenes(variante_id);
