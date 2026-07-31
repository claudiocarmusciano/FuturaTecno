-- Peso y dimensiones para cotizar envíos (Correo Argentino exige ambos en cualquier /rates).
-- Dos niveles: el producto puede tener su propio valor (override real), y si no lo tiene se
-- usa el default de su categoría (subcategoría primero, categoría padre como respaldo). Ningún
-- mayorista (Elit/Invid) ni Icecat con el plan actual traen este dato, así que arranca poblado
-- por default razonable y se corrige a mano donde haga falta.

ALTER TABLE productos ADD COLUMN peso_gramos INTEGER;
ALTER TABLE productos ADD COLUMN alto_cm INTEGER;
ALTER TABLE productos ADD COLUMN ancho_cm INTEGER;
ALTER TABLE productos ADD COLUMN largo_cm INTEGER;

ALTER TABLE categorias ADD COLUMN peso_gramos_default INTEGER;
ALTER TABLE categorias ADD COLUMN alto_cm_default INTEGER;
ALTER TABLE categorias ADD COLUMN ancho_cm_default INTEGER;
ALTER TABLE categorias ADD COLUMN largo_cm_default INTEGER;

-- Defaults por categoría principal (padre_id IS NULL). Se matchea por nombre, no por id: el
-- árbol ya no es fijo (se puede editar desde el admin desde V14... digo, desde que existe
-- CategoriaAdminController) y no hay garantía de que los ids sigan siendo los de la V10.
UPDATE categorias SET peso_gramos_default = 300, alto_cm_default = 20, ancho_cm_default = 15, largo_cm_default = 8
  WHERE nombre = 'Accesorios' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 50, alto_cm_default = 10, ancho_cm_default = 8, largo_cm_default = 3
  WHERE nombre = 'Almacenamiento' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 400, alto_cm_default = 20, ancho_cm_default = 15, largo_cm_default = 8
  WHERE nombre = 'Conectividad' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 300, alto_cm_default = 15, ancho_cm_default = 10, largo_cm_default = 8
  WHERE nombre = 'Consumibles' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 500, alto_cm_default = 20, ancho_cm_default = 20, largo_cm_default = 10
  WHERE nombre = 'Coolers' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 200, alto_cm_default = 15, ancho_cm_default = 10, largo_cm_default = 3
  WHERE nombre = 'Discos Rígidos / SSD' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 3000, alto_cm_default = 40, ancho_cm_default = 30, largo_cm_default = 25
  WHERE nombre = 'Electrodomésticos' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 2000, alto_cm_default = 30, ancho_cm_default = 20, largo_cm_default = 15
  WHERE nombre = 'Energía' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 5000, alto_cm_default = 45, ancho_cm_default = 35, largo_cm_default = 25
  WHERE nombre = 'Impresoras' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 50, alto_cm_default = 15, ancho_cm_default = 10, largo_cm_default = 3
  WHERE nombre = 'Memorias RAM' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 150, alto_cm_default = 12, ancho_cm_default = 10, largo_cm_default = 5
  WHERE nombre = 'Microprocesadores' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 4000, alto_cm_default = 60, ancho_cm_default = 40, largo_cm_default = 15
  WHERE nombre = 'Monitores' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 35, ancho_cm_default = 30, largo_cm_default = 8
  WHERE nombre = 'Mothers' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 2200, alto_cm_default = 40, ancho_cm_default = 30, largo_cm_default = 8
  WHERE nombre = 'Notebooks' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 400, alto_cm_default = 20, ancho_cm_default = 15, largo_cm_default = 8
  WHERE nombre = 'Periféricos' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 1200, alto_cm_default = 30, ancho_cm_default = 15, largo_cm_default = 6
  WHERE nombre = 'Placas de video' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 2500, alto_cm_default = 35, ancho_cm_default = 28, largo_cm_default = 15
  WHERE nombre = 'Proyectores' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 3000, alto_cm_default = 40, ancho_cm_default = 30, largo_cm_default = 15
  WHERE nombre = 'Scanners' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 500, alto_cm_default = 25, ancho_cm_default = 18, largo_cm_default = 3
  WHERE nombre = 'Tablets' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 1000, alto_cm_default = 30, ancho_cm_default = 20, largo_cm_default = 15
  WHERE nombre = 'DESTACADOS' AND padre_id IS NULL;

-- Overrides de subcategoría: donde el default del padre queda demasiado lejos de la realidad
-- para al menos una parte del rubro (ej. un iPhone y una MacBook no entran en la misma caja).
UPDATE categorias SET peso_gramos_default = 300, alto_cm_default = 18, ancho_cm_default = 9, largo_cm_default = 3
  WHERE nombre = 'iPhone' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Apple' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 600, alto_cm_default = 28, ancho_cm_default = 20, largo_cm_default = 3
  WHERE nombre = 'iPad' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Apple' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 150, alto_cm_default = 12, ancho_cm_default = 10, largo_cm_default = 8
  WHERE nombre = 'Watch' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Apple' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 2000, alto_cm_default = 35, ancho_cm_default = 25, largo_cm_default = 3
  WHERE nombre = 'MacBook' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Apple' AND padre_id IS NULL);

UPDATE categorias SET peso_gramos_default = 8000, alto_cm_default = 50, ancho_cm_default = 25, largo_cm_default = 50
  WHERE nombre IN ('PC', 'Kit PC') AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 6000, alto_cm_default = 55, ancho_cm_default = 40, largo_cm_default = 15
  WHERE nombre = 'All in One' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1000, alto_cm_default = 20, ancho_cm_default = 20, largo_cm_default = 8
  WHERE nombre = 'Mini PC' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);

UPDATE categorias SET peso_gramos_default = 3000, alto_cm_default = 42, ancho_cm_default = 32, largo_cm_default = 10
  WHERE nombre = 'Gamer' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Notebooks' AND padre_id IS NULL);

UPDATE categorias SET peso_gramos_default = 6000, alto_cm_default = 50, ancho_cm_default = 25, largo_cm_default = 50
  WHERE nombre IN ('Gabinetes con Fuente', 'Gabinetes sin Fuente')
  AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 20, ancho_cm_default = 15, largo_cm_default = 10
  WHERE nombre = 'Fuentes de Alimentación' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);

UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 25, ancho_cm_default = 20, largo_cm_default = 20
  WHERE nombre = 'Parlantes' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);

-- "Sillas y escritorios" era una hoja sin subcategorías (V10) mezclando dos productos con cajas
-- muy distintas. Se abre en dos hojas nuevas con las medidas reales (vienen desarmados en caja,
-- confirmado por el negocio) y se migran los productos existentes.
INSERT INTO categorias (nombre, padre_id, peso_gramos_default, alto_cm_default, ancho_cm_default, largo_cm_default, created_at, updated_at)
VALUES (
  'Sillas',
  (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL),
  20000, 85, 40, 70, now(), now()
);
INSERT INTO categorias (nombre, padre_id, peso_gramos_default, alto_cm_default, ancho_cm_default, largo_cm_default, created_at, updated_at)
VALUES (
  'Escritorios',
  (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL),
  16000, 120, 70, 10, now(), now()
);

-- Todo lo que hoy cuelga directo de "Sillas y escritorios" son sillas (los 2 únicos productos
-- cargados en esa categoría al momento de escribir esta migración); se reasignan a la hoja nueva.
UPDATE productos SET categoria_id = (
  SELECT id FROM categorias WHERE nombre = 'Sillas'
    AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL)
)
WHERE categoria_id = (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL);
