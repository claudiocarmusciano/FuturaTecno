-- La búsqueda de imágenes ahora consulta también el catálogo en vivo (ImagenManualService#buscar),
-- porque el 98% de los productos tiene foto y las tablas de memoria cubrían una fracción mínima:
-- solo se escriben al editar a mano, al encontrar buscando o al dar de baja, nunca cuando el
-- artículo entra con imagen desde un mayorista o una carga JSON.
--
-- Esa consulta compara marca y modelo NORMALIZADOS, y una expresión no usa el índice común: sin
-- esto es un recorrido completo de productos por cada artículo del listado. Con 40 artículos y
-- 3.200 productos son 128.000 filas leídas para no gastar un crédito de API — el ahorro se comía
-- a sí mismo. El índice funcional replica exactamente la expresión de la consulta; si alguna vez
-- se cambia la normalización en Java, hay que cambiarla en los dos lados o el índice deja de usarse
-- en silencio (la consulta sigue andando, solo que lenta).
CREATE INDEX idx_productos_marca_modelo_norm ON productos (
    lower(regexp_replace(trim(marca), '\s+', ' ', 'g')),
    lower(regexp_replace(trim(modelo), '\s+', ' ', 'g'))
);
