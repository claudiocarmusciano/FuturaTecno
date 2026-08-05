-- V19 no alcanzó los registros importados porque el identificador externo persistido no coincidía
-- con el esperado. Estos son los IDs reales expuestos por el catálogo para la importación Apple
-- del 05/08/2026. Se actualizan exclusivamente esos productos y su galería.
UPDATE productos
SET imagen_url = CASE id
    WHEN 2779 THEN 'https://www.apple.com/newsroom/images/2023/09/apple-debuts-iphone-15-and-iphone-15-plus/article/Apple-iPhone-15-lineup-hero-230912_inline.jpg.large.jpg'
    WHEN 2780 THEN 'https://www.apple.com/newsroom/images/2024/09/apple-introduces-iphone-16-and-iphone-16-plus/article/geo/Apple-iPhone-16-hero-geo-240909_inline.jpg.large.jpg'
    WHEN 2781 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-iphone-17/article/Apple-iPhone-17-hero-250909_inline.jpg.large.jpg'
    WHEN 2782 THEN 'https://www.apple.com/newsroom/images/2025/09/introducing-iphone-air-a-powerful-new-iphone-with-a-breakthrough-design/article/Apple-iPhone-Air-color-lineup-250909_big.jpg.large.jpg'
    WHEN 2783 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2784 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2785 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2786 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2787 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2788 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2789 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2790 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2791 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-unveils-iphone-17-pro-and-iphone-17-pro-max/article/Apple-iPhone-17-Pro-cosmic-orange-250909_inline.jpg.large.jpg'
    WHEN 2898 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-introduces-apple-watch-se-3/article/Apple-Watch-SE-3-hero-250909_big.jpg.large.jpg'
    WHEN 2899 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-apple-watch-series-11-featuring-groundbreaking-health-insights/article/Apple-Watch-Series-11-hero-250909_big.jpg.large.jpg'
    WHEN 2900 THEN 'https://www.apple.com/newsroom/images/2025/09/apple-debuts-apple-watch-series-11-featuring-groundbreaking-health-insights/article/Apple-Watch-Series-11-hero-250909_big.jpg.large.jpg'
    WHEN 2901 THEN 'https://www.apple.com/newsroom/images/2025/09/introducing-apple-watch-ultra-3/article/Apple-Watch-Ultra-3-hero-250909_big.jpg.large.jpg'
END
WHERE id IN (2779, 2780, 2781, 2782, 2783, 2784, 2785, 2786, 2787, 2788, 2789, 2790, 2791, 2898, 2899, 2900, 2901);

UPDATE imagenes galeria
SET url = p.imagen_url
FROM productos p
WHERE galeria.producto_id = p.id
  AND p.id IN (2779, 2780, 2781, 2782, 2783, 2784, 2785, 2786, 2787, 2788, 2789, 2790, 2791, 2898, 2899, 2900, 2901);
