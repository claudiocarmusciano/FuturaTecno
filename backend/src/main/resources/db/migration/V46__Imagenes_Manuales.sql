-- Fotos de galería cargadas a mano en Admin → Imágenes (2026-10-15). La sincronización de Elit y la
-- carga JSON reemplazan la galería del producto en cada corrida; con esta marca, las manuales se
-- conservan.
ALTER TABLE imagenes ADD COLUMN manual BOOLEAN NOT NULL DEFAULT FALSE;
