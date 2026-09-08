-- Evita que el buscador automático repita siempre los mismos productos sin resultado.
-- Es independiente de updated_at, que representa actualizaciones comerciales del artículo.
ALTER TABLE productos ADD COLUMN imagen_busqueda_at TIMESTAMP;

CREATE INDEX idx_productos_imagen_busqueda_at
    ON productos (imagen_busqueda_at);
