ALTER TABLE usuarios ADD COLUMN bases_aceptadas_en TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN instagram_usuario VARCHAR(30);
ALTER TABLE usuarios ADD COLUMN instagram_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN codigo_sorteo VARCHAR(24);
ALTER TABLE usuarios ADD COLUMN codigo_sorteo_asignado_en TIMESTAMP;

CREATE UNIQUE INDEX idx_usuarios_codigo_sorteo_unico ON usuarios(codigo_sorteo) WHERE codigo_sorteo IS NOT NULL;
