-- Los enlaces recibidos en la importación JSON de Apple eran miniaturas temporales de buscadores
-- y dejaron de cargar. Se reemplazan solamente los 17 SKU afectados del proveedor COZ con
-- renders publicados por Apple, que son URLs directas y estables. La galería se alinea con la
-- principal para no conservar una segunda URL inválida en la ficha del producto.
WITH imagenes_validas(codigo_externo, url) AS (
    VALUES
        ('P2779', 'https://www.apple.com/newsroom/images/2023/09/apple-debuts-iphone-15-and-iphone-15-plus/article/Apple-iPhone-15-lineup-hero-230912_inline.jpg.large.jpg'),
        ('P2780', 'https://www.apple.com/newsroom/images/2024/09/apple-introduces-iphone-16-and-iphone-16-plus/article/geo/Apple-iPhone-16-hero-geo-240909_inline.jpg.large.jpg'),
        ('P2781', 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-iphone-17/article/Apple-iPhone-17-hero-250909_inline.jpg.large.jpg'),
        ('P2782', 'https://www.apple.com/newsroom/images/2025/09/introducing-iphone-air-a-powerful-new-iphone-with-a-breakthrough-design/article/Apple-iPhone-Air-color-lineup-250909_big.jpg.large.jpg'),
        ('P2783', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2784', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2785', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2786', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2787', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2788', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2789', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2790', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2791', 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'),
        ('P2898', 'https://www.apple.com/newsroom/images/2025/09/apple-introduces-apple-watch-se-3/article/Apple-Watch-SE-3-hero-250909_big.jpg.large.jpg'),
        ('P2899', 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-apple-watch-series-11-featuring-groundbreaking-health-insights/article/Apple-Watch-Series-11-hero-250909_big.jpg.large.jpg'),
        ('P2900', 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-apple-watch-series-11-featuring-groundbreaking-health-insights/article/Apple-Watch-Series-11-hero-250909_big.jpg.large.jpg'),
        ('P2901', 'https://www.apple.com/newsroom/images/2025/09/introducing-apple-watch-ultra-3/article/Apple-Watch-Ultra-3-hero-250909_big.jpg.large.jpg')
)
UPDATE productos p
SET imagen_url = i.url
FROM imagenes_validas i
JOIN proveedores pr ON pr.id = p.proveedor_id
WHERE pr.codigo = 'COZ'
  AND p.codigo_externo = i.codigo_externo;

UPDATE imagenes galeria
SET url = p.imagen_url
FROM productos p
JOIN proveedores pr ON pr.id = p.proveedor_id
WHERE galeria.producto_id = p.id
  AND pr.codigo = 'COZ'
  AND p.codigo_externo IN (
      'P2779', 'P2780', 'P2781', 'P2782', 'P2783', 'P2784', 'P2785', 'P2786', 'P2787',
      'P2788', 'P2789', 'P2790', 'P2791', 'P2898', 'P2899', 'P2900', 'P2901'
  );
