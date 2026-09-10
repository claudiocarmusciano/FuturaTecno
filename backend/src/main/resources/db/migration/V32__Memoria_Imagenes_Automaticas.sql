CREATE TABLE imagenes_automaticas (
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    url TEXT NOT NULL,
    PRIMARY KEY (marca, modelo)
);

-- Recupera imágenes históricas; las manuales mantienen prioridad en la aplicación.
INSERT INTO imagenes_automaticas (marca, modelo, url)
SELECT DISTINCT ON (lower(regexp_replace(trim(marca), '\s+', ' ', 'g')),
                    lower(regexp_replace(trim(modelo), '\s+', ' ', 'g')))
       lower(regexp_replace(trim(marca), '\s+', ' ', 'g')),
       lower(regexp_replace(trim(modelo), '\s+', ' ', 'g')), imagen_url
FROM productos
WHERE nullif(trim(marca), '') IS NOT NULL AND nullif(trim(modelo), '') IS NOT NULL
  AND nullif(trim(imagen_url), '') IS NOT NULL
ORDER BY lower(regexp_replace(trim(marca), '\s+', ' ', 'g')),
         lower(regexp_replace(trim(modelo), '\s+', ' ', 'g')), id DESC;

-- Cubre todos los importadores y guardados, incluso SQL externo.
CREATE FUNCTION recordar_imagen_producto() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF nullif(trim(NEW.imagen_url), '') IS NOT NULL
       AND nullif(trim(NEW.marca), '') IS NOT NULL
       AND nullif(trim(NEW.modelo), '') IS NOT NULL THEN
        INSERT INTO imagenes_automaticas (marca, modelo, url)
        VALUES (lower(regexp_replace(trim(NEW.marca), '\s+', ' ', 'g')),
                lower(regexp_replace(trim(NEW.modelo), '\s+', ' ', 'g')), NEW.imagen_url)
        ON CONFLICT (marca, modelo) DO UPDATE SET url = EXCLUDED.url;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER recordar_imagen_producto
AFTER INSERT OR UPDATE OF imagen_url ON productos
FOR EACH ROW EXECUTE FUNCTION recordar_imagen_producto();
