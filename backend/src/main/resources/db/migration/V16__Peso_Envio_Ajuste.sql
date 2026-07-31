-- Corrección de los defaults de peso/dimensiones que sembró la V14.
--
-- Síntoma: en producción (2026-07-31) un switch rackeable de 48 puertos, un volante con pedales y
-- un paquete de 200 g cotizaban todos $11.067,88 a domicilio a CP 5000. Los defaults de la V14 se
-- eligieron pensando en el producto chico de cada rubro, así que cualquier bulto grande caía en el
-- mismo escalón que el más chico y la diferencia la absorbía el negocio. Andreani sí discrimina:
-- el mismo destino con un bulto de 20 kg / 60×50×40 cotiza $91.291,35.
--
-- Criterio nuevo, y es lo que cambia respecto de la V14: **el default de una categoría se elige en
-- el extremo pesado de lo que esa categoría contiene, no en el promedio.** Los dos errores no
-- cuestan lo mismo: subestimar es plata que pone el negocio en cada venta y no se entera; sobrestimar,
-- como mucho, cotiza de más en un producto puntual, que se corrige con el override del producto.
--
-- Tres cosas que se arreglan acá:
--   1) Defaults de categoría más realistas, sobre todo en las hojas que hasta ahora heredaban un
--      default del padre pensado para otra cosa (UPS heredaba "Energía" 2 kg; los switches
--      administrables heredaban "Conectividad" 400 g).
--   2) Overrides por producto para los outliers que no entran en ningún default razonable de su
--      categoría (los volantes en Accesorios, los minicomponentes LG en Parlantes, los packs de
--      batería en UPS, los monitores de 45"/49"). Se matchean por (proveedor, codigo_externo), que
--      es la misma clave de dedup de la importación, así que sobreviven a la sync diaria — la sync
--      no toca estas cuatro columnas, solo las escribe ProductoAdminService.
--   3) Defaults para tres categorías padre que la V14 dejó en NULL ('Computadoras',
--      'Gabinetes y Fuentes', 'Sillas y escritorios'), porque solo les cargó las hojas. Eso ya
--      dejaba un agujero real: la hoja 'Kit Gabinete, teclado, mouse y parlante' no tenía default
--      propio ni padre del cual heredar, así que un producto ahí adentro no resuelve medidas y
--      EnvioService devuelve "no disponible" para el carrito entero, no solo para ese ítem. Está
--      vacía hoy, pero era una bomba de tiempo. Después de esta migración no queda ninguna
--      categoría del árbol sin las cuatro medidas resolubles.
--
-- Cuando se toca una hoja se le cargan los cuatro campos juntos a propósito: EnvioService resuelve
-- campo por campo, así que una hoja con solo el peso cargado arma un bulto Frankenstein (5 kg
-- dentro de la caja de 20×15×8 del padre).
--
-- Los valores salen de las especificaciones de fábrica de lo que hay hoy en catálogo, redondeadas
-- para arriba a peso de bulto embalado. No están medidos con balanza: son un piso razonable para
-- dejar de perder plata, y se corrigen contra el bulto real a medida que se despachan.

-- ---------------------------------------------------------------------------------------------
-- 1. Categorías padre
-- ---------------------------------------------------------------------------------------------

-- Grab-bag real: hoy convive un capturadora de 300 g con cinco volantes de 11 kg. Los volantes van
-- por override más abajo; el default sube a un "accesorio grande" para que el próximo que entre no
-- salga cotizado como un sobre.
UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 30, ancho_cm_default = 25, largo_cm_default = 15
  WHERE nombre = 'Accesorios' AND padre_id IS NULL;

-- Sube poco: el padre solo cubre cables, fichas y adaptadores USB. Todo lo pesado del rubro
-- (switches, OLTs, routers, mesh) pasa a tener default propio por hoja más abajo.
UPDATE categorias SET peso_gramos_default = 600, alto_cm_default = 25, ancho_cm_default = 20, largo_cm_default = 10
  WHERE nombre = 'Conectividad' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 4000, alto_cm_default = 30, ancho_cm_default = 25, largo_cm_default = 20
  WHERE nombre = 'Energía' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 8000, alto_cm_default = 50, ancho_cm_default = 45, largo_cm_default = 35
  WHERE nombre = 'Impresoras' AND padre_id IS NULL;

-- Un kit de 2×16 GB embalado no pesa 50 g. Sigue siendo el default más liviano del árbol.
UPDATE categorias SET peso_gramos_default = 200, alto_cm_default = 18, ancho_cm_default = 12, largo_cm_default = 4
  WHERE nombre = 'Memorias RAM' AND padre_id IS NULL;

-- Los 150 g eran el micro pelado; el boxed con cooler (los Ryzen que hay en catálogo) va a ~500 g.
UPDATE categorias SET peso_gramos_default = 500, alto_cm_default = 15, ancho_cm_default = 12, largo_cm_default = 10
  WHERE nombre = 'Microprocesadores' AND padre_id IS NULL;

-- El default pasa a ser un 27" embalado (~6,5 kg), que es el grueso del catálogo. Los 32"/34" y los
-- ultrawide de 45"/49" van por override: no hay un solo número que cubra de 24" a 49".
UPDATE categorias SET peso_gramos_default = 6500, alto_cm_default = 45, ancho_cm_default = 70, largo_cm_default = 18
  WHERE nombre = 'Monitores' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 1800, alto_cm_default = 35, ancho_cm_default = 30, largo_cm_default = 8
  WHERE nombre = 'Mothers' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 3000, alto_cm_default = 45, ancho_cm_default = 32, largo_cm_default = 10
  WHERE nombre = 'Notebooks' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 700, alto_cm_default = 25, ancho_cm_default = 20, largo_cm_default = 10
  WHERE nombre = 'Periféricos' AND padre_id IS NULL;

-- Una RTX 5080 Gaming Trio embalada va a ~2,8 kg en una caja de 40 cm, no 1,2 kg en una de 30.
UPDATE categorias SET peso_gramos_default = 2500, alto_cm_default = 40, ancho_cm_default = 25, largo_cm_default = 10
  WHERE nombre = 'Placas de video' AND padre_id IS NULL;

UPDATE categorias SET peso_gramos_default = 4500, alto_cm_default = 40, ancho_cm_default = 35, largo_cm_default = 20
  WHERE nombre = 'Proyectores' AND padre_id IS NULL;

-- Las tablets del catálogo vienen casi todas en combo con teclado folio y lápiz: ~1,3 kg de bulto.
UPDATE categorias SET peso_gramos_default = 1300, alto_cm_default = 30, ancho_cm_default = 22, largo_cm_default = 8
  WHERE nombre = 'Tablets' AND padre_id IS NULL;

-- Acá el default es casi decorativo: lo único cargado es una heladera de 395 lts, que va por
-- override y ni siquiera es un envío de paquetería. 25 kg es "electrodoméstico mediano" para que lo
-- próximo que entre no cotice como un microondas de juguete.
UPDATE categorias SET peso_gramos_default = 25000, alto_cm_default = 60, ancho_cm_default = 50, largo_cm_default = 50
  WHERE nombre = 'Electrodomésticos' AND padre_id IS NULL;

-- DESTACADOS no es un rubro, es una vidriera: hoy tiene desde una SODIMM hasta un escritorio gamer
-- de 35 kg, y como categoria_id es único, el producto pierde su categoría real mientras está
-- destacado. Ningún default puede estar bien acá — se sube a 5 kg como red de contención y todo lo
-- que hay hoy va por override.
UPDATE categorias SET peso_gramos_default = 5000, alto_cm_default = 40, ancho_cm_default = 35, largo_cm_default = 25
  WHERE nombre = 'DESTACADOS' AND padre_id IS NULL;

-- Padres que la V14 dejó en NULL. Ver punto (3) del encabezado.
UPDATE categorias SET peso_gramos_default = 12000, alto_cm_default = 55, ancho_cm_default = 30, largo_cm_default = 50
  WHERE nombre = 'Computadoras' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 14000, alto_cm_default = 60, ancho_cm_default = 35, largo_cm_default = 60
  WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL;
UPDATE categorias SET peso_gramos_default = 20000, alto_cm_default = 85, ancho_cm_default = 40, largo_cm_default = 70
  WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL;

-- 'Almacenamiento' (50 g), 'Discos Rígidos / SSD' (200 g), 'Coolers' (500 g), 'Consumibles' (300 g)
-- y 'Scanners' (3 kg) se dejan como estaban: el reporte confirmó que el Seagate de 8 TB cotizaba
-- bien, y esos rubros no tienen productos fuera de escala salvo los que se corrigen por hoja abajo.

-- ---------------------------------------------------------------------------------------------
-- 2. Hojas
-- ---------------------------------------------------------------------------------------------

-- Conectividad: el rubro va de un adaptador USB a un chasis GPON rackeable. Es la categoría que
-- disparó el reporte, así que cada hoja con producto cargado pasa a tener default propio.
UPDATE categorias SET peso_gramos_default = 1600, alto_cm_default = 22, ancho_cm_default = 30, largo_cm_default = 12
  WHERE nombre = 'Access Point y Extensores de Rango' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
-- Un SG3452P (48 puertos PoE) embalado ronda los 7 kg en caja de rack 1U.
UPDATE categorias SET peso_gramos_default = 7000, alto_cm_default = 12, ancho_cm_default = 54, largo_cm_default = 34
  WHERE nombre = 'Switches Administrables' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
-- Esta hoja mezcla el TL-SG1048 de 48 puertos con los switches de escritorio de 5/8 puertos, así que
-- el default queda en el medio y el de 48 puertos lleva override.
UPDATE categorias SET peso_gramos_default = 2500, alto_cm_default = 12, ancho_cm_default = 40, largo_cm_default = 30
  WHERE nombre = 'Switches No Administrables' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 5000, alto_cm_default = 15, ancho_cm_default = 50, largo_cm_default = 35
  WHERE nombre = 'Modem ADSL y GPON' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 2000, alto_cm_default = 10, ancho_cm_default = 35, largo_cm_default = 28
  WHERE nombre = 'Router' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1800, alto_cm_default = 28, ancho_cm_default = 25, largo_cm_default = 15
  WHERE nombre = 'Router Wireless' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 600, alto_cm_default = 15, ancho_cm_default = 15, largo_cm_default = 12
  WHERE nombre = 'Cámaras IP' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 800, alto_cm_default = 15, ancho_cm_default = 20, largo_cm_default = 12
  WHERE nombre = 'POE (Power Over Ethernet)' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Conectividad' AND padre_id IS NULL);

-- Consumibles: la hoja HP son 83 productos y la mayoría es toner LaserJet (~1,2 kg embalado), no
-- cartuchos de tinta. Cartuchos y Tintas se fijan explícitos para que no arrastren ese peso.
UPDATE categorias SET peso_gramos_default = 1200, alto_cm_default = 15, ancho_cm_default = 40, largo_cm_default = 22
  WHERE nombre = 'Consumibles HP' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Consumibles' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 250, alto_cm_default = 8, ancho_cm_default = 18, largo_cm_default = 15
  WHERE nombre IN ('Cartuchos', 'Tintas') AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Consumibles' AND padre_id IS NULL);

-- Un AIO de 360 mm embalado ronda 2,8 kg en una caja de 45 cm; el default de 'Coolers' (500 g) era
-- el de un fan suelto.
UPDATE categorias SET peso_gramos_default = 2800, alto_cm_default = 15, ancho_cm_default = 45, largo_cm_default = 20
  WHERE nombre = 'Watercoolers' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Coolers' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 500, alto_cm_default = 12, ancho_cm_default = 15, largo_cm_default = 15
  WHERE nombre = 'Fans' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Coolers' AND padre_id IS NULL);

-- Los discos internos quedan en los 200 g del padre (confirmado OK con el Seagate de 8 TB); el
-- externo lleva fuente y cable.
UPDATE categorias SET peso_gramos_default = 400, alto_cm_default = 5, ancho_cm_default = 18, largo_cm_default = 12
  WHERE nombre = 'Disco Rígido Externo' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Discos Rígidos / SSD' AND padre_id IS NULL);

-- UPS: el peor caso de todos. La hoja no tenía default propio y heredaba los 2 kg de 'Energía',
-- cuando un online de 3 KVA pesa 30 kg y un pack de baterías pasa los 100. El default queda en un
-- online de 1-2 KVA y todo lo demás va por override, incluida la tarjeta SNMP (300 g) que si no
-- heredaría los 18 kg de la hoja — el error espejo del que estamos arreglando.
UPDATE categorias SET peso_gramos_default = 18000, alto_cm_default = 45, ancho_cm_default = 25, largo_cm_default = 40
  WHERE nombre = 'UPS' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Energía' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 4000, alto_cm_default = 20, ancho_cm_default = 30, largo_cm_default = 25
  WHERE nombre = 'Estabilizadores' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Energía' AND padre_id IS NULL);

-- Gabinetes: un mid-tower tipo Corsair 5000T o TT Tower 600 embalado va de 13 a 16 kg, no 6.
UPDATE categorias SET peso_gramos_default = 14000, alto_cm_default = 60, ancho_cm_default = 35, largo_cm_default = 60
  WHERE nombre = 'Gabinetes sin Fuente' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 15000, alto_cm_default = 60, ancho_cm_default = 35, largo_cm_default = 60
  WHERE nombre = 'Gabinetes con Fuente' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 3000, alto_cm_default = 15, ancho_cm_default = 30, largo_cm_default = 25
  WHERE nombre = 'Fuentes de Alimentación' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 16000, alto_cm_default = 60, ancho_cm_default = 40, largo_cm_default = 60
  WHERE nombre = 'Kit Gabinete, teclado, mouse y parlante' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Gabinetes y Fuentes' AND padre_id IS NULL);

-- Impresoras: las multifunción láser color y las A3 son otro animal que una Ink Jet de escritorio.
UPDATE categorias SET peso_gramos_default = 8000, alto_cm_default = 35, ancho_cm_default = 50, largo_cm_default = 45
  WHERE nombre = 'Ink Jet' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Impresoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 8000, alto_cm_default = 30, ancho_cm_default = 45, largo_cm_default = 40
  WHERE nombre = 'Laser' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Impresoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 12000, alto_cm_default = 45, ancho_cm_default = 55, largo_cm_default = 50
  WHERE nombre = 'Multifunción' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Impresoras' AND padre_id IS NULL);

-- Computadoras: 'Kit PC' compartía los 8 kg de 'PC' por estar en el mismo IN de la V14, pero acá un
-- kit es mother + memoria + SSD (~2,5 kg). Ese sí era un cobro de más.
UPDATE categorias SET peso_gramos_default = 12000, alto_cm_default = 55, ancho_cm_default = 30, largo_cm_default = 50
  WHERE nombre = 'PC' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 2500, alto_cm_default = 12, ancho_cm_default = 35, largo_cm_default = 30
  WHERE nombre = 'Kit PC' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 10, ancho_cm_default = 25, largo_cm_default = 22
  WHERE nombre = 'Mini PC' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 12000, alto_cm_default = 45, ancho_cm_default = 70, largo_cm_default = 25
  WHERE nombre = 'All in One' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Computadoras' AND padre_id IS NULL);

UPDATE categorias SET peso_gramos_default = 4000, alto_cm_default = 14, ancho_cm_default = 48, largo_cm_default = 36
  WHERE nombre = 'Gamer' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Notebooks' AND padre_id IS NULL);

-- Periféricos: un teclado mecánico full-size embalado (1,5 kg, caja de 50 cm) no entra en los 400 g
-- del padre. Los parlantes suben, pero los minicomponentes LG de 12-38 kg van igual por override.
UPDATE categorias SET peso_gramos_default = 1000, alto_cm_default = 12, ancho_cm_default = 25, largo_cm_default = 22
  WHERE nombre = 'Auriculares' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1500, alto_cm_default = 8, ancho_cm_default = 50, largo_cm_default = 25
  WHERE nombre = 'Teclados' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1800, alto_cm_default = 10, ancho_cm_default = 50, largo_cm_default = 30
  WHERE nombre = 'Teclado + Mouse' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 400, alto_cm_default = 8, ancho_cm_default = 15, largo_cm_default = 12
  WHERE nombre = 'Mouse' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 900, alto_cm_default = 8, ancho_cm_default = 40, largo_cm_default = 15
  WHERE nombre = 'Mousepads' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 2500, alto_cm_default = 25, ancho_cm_default = 30, largo_cm_default = 25
  WHERE nombre = 'Parlantes' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 1200, alto_cm_default = 25, ancho_cm_default = 20, largo_cm_default = 20
  WHERE nombre = 'Micrófonos' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 400, alto_cm_default = 8, ancho_cm_default = 18, largo_cm_default = 12
  WHERE nombre = 'Web Cam' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 500, alto_cm_default = 5, ancho_cm_default = 18, largo_cm_default = 12
  WHERE nombre = 'Power Banks' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Periféricos' AND padre_id IS NULL);

-- Las sillas Raptor Throne embaladas dan ~22 kg; el escritorio Glow Station, ~32 kg (tablero + patas
-- con elevación, en una caja de 130 cm). La V14 los había puesto en 20 y 16 kg.
UPDATE categorias SET peso_gramos_default = 23000
  WHERE nombre = 'Sillas' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL);
UPDATE categorias SET peso_gramos_default = 32000, alto_cm_default = 20, ancho_cm_default = 130, largo_cm_default = 70
  WHERE nombre = 'Escritorios' AND padre_id = (SELECT id FROM categorias WHERE nombre = 'Sillas y escritorios' AND padre_id IS NULL);

-- ---------------------------------------------------------------------------------------------
-- 3. Overrides por producto
-- ---------------------------------------------------------------------------------------------
-- Los outliers que no entran en ningún default razonable de su categoría. Se matchean por
-- (proveedor.codigo, producto.codigo_externo) — la misma clave con la que dedupea la importación —
-- así que un producto que todavía no está cargado simplemente no matchea ninguna fila y no rompe
-- nada. Estos mismos valores se pueden editar después desde Admin → Productos.
WITH overrides(proveedor_codigo, codigo_externo, peso, alto, ancho, largo) AS (
    VALUES
    -- Accesorios: los volantes son el caso testigo del reporte (cotizaban como un paquete de 300 g).
    ('INV', '0413076',  11000,  40,  50,  60),  -- Volante Logitech G923 Trueforce (volante + pedalera)
    ('INV', '0407612',  11000,  40,  50,  60),  -- Volante Logitech G29 Racing
    ('INV', '0418612',  12000,  40,  50,  60),  -- Volante Logitech G29 SE Racing + Palanca
    ('INV', '0418280',  11000,  40,  50,  60),  -- Volante Logitech RS50 System
    ('INV', '0417853',   9000,  35,  45,  55),  -- Volante Genius Speed Master X2 c/palanca
    ('INV', '0418405',   5000,  30,  35,  40),  -- Pedalera Logitech p/ Volante RS
    ('INV', '0418063',   2500,  25,  20,  30),  -- Palanca Shifter/Handbrake Logitech RS/PRO
    ('INV', '0418195',   2500,  30,  25,  25),  -- Luz proyección JBL Party Light Beam
    ('INV', '0415600',    700,  10,  22,  18),  -- Stream Deck+ Elgato (baja respecto del default nuevo)
    ('INV', '0418571',    400,   8,  20,  15),  -- Capturadora Elgato Game Capture 4K S (idem)

    -- DESTACADOS: la vidriera. Cada uno con el peso de su rubro real.
    ('INV', '0418177',  35000,  20, 130,  70),  -- Escritorio Gamer Raptor Glow Station RGB
    ('INV', '0417335',  22000,  85,  40,  70),  -- Silla Gamer Raptor Throne R1 Tela Gris
    ('INV', '0417812',  18000,  55,  60,  55),  -- Sist. Kelyx Ryzen 5 + Monitor 24" (gabinete + monitor)
    ('INV', '0417950',   8000,  35,  50,  45),  -- Epson L4360 (MF)
    ('INV', '0418266',   1800,  10,  35,  20),  -- VGA Gigabyte RTX 5050 WINDFORCE OC 8G
    ('INV', '0417019',   2600,  10,  40,  25),  -- VGA Gigabyte Radeon RX 9070 GAMING OC 16G
    ('INV', '0418332',   1500,   8,  30,  20),  -- VGA PNY QUADRO RTX PRO 4000 Blackwell
    ('INV', '0416371',     80,   4,  15,  10),  -- Memoria SODIMM DDR4 Kingston 32Gb
    ('INV', '0416309',   1200,  15,  40,  22),  -- Toner HP 78A CE278A
    ('INV', '0417709',    900,  12,  25,  22),  -- Auricular JBL Tour One M3 Arena
    ('INV', '0417708',    900,  12,  25,  22),  -- Auricular JBL Tour One M3 Negro
    ('INV', '0416442',   1000,  12,  25,  22),  -- Auricular JBL Quantum 610
    ('INV', '0418382',   2000,  20,  25,  25),  -- Parlante Harman Kardon Luna Gris
    ('INV', '0418381',   2000,  20,  25,  25),  -- Parlante Harman Kardon Luna Negro

    -- El switch de 48 puertos del reporte, dentro de una hoja que también tiene switches de 5 puertos.
    ('INV', '0407746',   4500,  12,  54,  34),  -- TP-Link TL-SG1048 Switch Gigabit 48P

    -- Impresoras grandes: A3 y multifunción láser color.
    ('INV', '0416994',  25000,  50,  55,  55),  -- HP LaserJet Pro Color MFP 3303fdw
    ('INV', '0412880',  14000,  45,  60,  55),  -- Epson L14150 (A3)
    ('INV', '0413731',  14000,  45,  60,  55),  -- Epson L8180 (A3)
    ('INV', '0417222',  16000,  45,  60,  55),  -- HP OfficeJet Pro 9730 WF (A3)

    -- Monitores de más de 30": el default de la categoría es un 27".
    ('INV', '0417726',   8500,  50,  80,  20),  -- LG 32 UltraFine 32UR550K 4K
    ('INV', '0417462',  11000,  50,  80,  25),  -- LG 32 Ergo UltraFine 32UN880K-B (pie ergo pesado)
    ('INV', '0414581',   9500,  45,  85,  20),  -- LG 34 UltraWide 34WP500-B
    ('INV', '0414775',  12000,  50,  80,  25),  -- LG 28 Dual Ergo 28MQ780-B (dos paneles + brazo)
    ('INV', '0417861',  18000,  55, 125,  30),  -- LG 49 UltraWide 49U950A-W curvo
    ('INV', '0414580',   6000,  40,  70,  18),  -- LG 26 UltraWide 26WQ500-B
    ('INV', '0416171',   9500,  50,  80,  22),  -- LG 32 UltraGear 32GR93U
    ('INV', '0416172',  16000,  55, 115,  30),  -- LG 45 UltraGear 45GR75DC curvo
    ('INV', '0416173',  15000,  55, 115,  30),  -- LG 45 UltraGear 45GS95QE curvo OLED
    ('INV', '0416504',   9000,  50,  80,  22),  -- LG 32 UltraGear 32GS85Q-B

    -- Audio LG: los minicomponentes y torres de fiesta no son "parlantes" en el sentido del default.
    ('INV', '0417842',  38000,  90,  40,  40),  -- Minicomponente LG XBOOM CL88 2900 W
    ('INV', '0417841',  20000,  70,  35,  35),  -- Minicomponente LG XBOOM CJ45 720 W
    ('INV', '0417840',  12000,  55,  32,  32),  -- Minicomponente LG XBOOM CK43N 300 W
    ('INV', '0417839',  22000,  75,  38,  38),  -- Parlante LG XBOOM OK99M 2000 W
    ('INV', '0417838',  11000,  60,  35,  35),  -- Parlante portátil LG XBOOM Stage 301
    ('INV', '0418566',   6000,  20, 100,  15),  -- Soundbar JBL 500 M2

    -- UPS Hikvision. Los packs de batería (x16 / x20) superan de largo lo que Andreani despacha como
    -- paquete: con estos valores la cotización va a volver vacía y el checkout cae en "a cotizar",
    -- que es exactamente lo que corresponde para un bulto de 120 kg.
    ('INV', '0416746',  14000,  22,  40,  35),  -- DS-UPS2000 backup 2000VA
    ('INV', '0416747',  18000,  22,  45,  38),  -- DS-UPS3000-x backup 3000VA
    ('INV', '0418009',  14000,  22,  45,  40),  -- DS-UPS01K24-R online 1KVA
    ('INV', '0418010',  22000,  25,  48,  42),  -- DS-UPS02K48-R online 2KVA
    ('INV', '0418011',  30000,  25,  50,  45),  -- DS-UPS03K72-R online 3KVA
    ('INV', '0418416',  16000,  25,  48,  42),  -- DS-UPS01K24-R/TJS rack tower 1KVA c/bat
    ('INV', '0418417',  24000,  25,  50,  45),  -- DS-UPS02K48-R/TJS rack tower 2KVA c/bat
    ('INV', '0418423',  32000,  25,  52,  48),  -- DS-UPS03K72-R/TJS rack tower 3KVA c/bat
    ('INV', '0418418',  20000,  25,  55,  50),  -- DS-UPS06K-R/TJL rack tower 6KVA sin bat
    ('INV', '0418419',  28000,  30,  60,  55),  -- DS-UPS10K-R/TJL rack tower 10KVA sin bat
    ('INV', '0418420', 120000,  50,  60,  45),  -- Pack de batería 192Vdc (x16 bat)
    ('INV', '0418421', 150000,  55,  65,  50),  -- Pack de batería 240Vdc (x20 bat)
    ('INV', '0418422',  32000,  30,  45,  35),  -- Pack de batería 24Vcc 1KVA (x4 bat)
    ('INV', '0418426',    300,   5,  15,  12),  -- Tarjeta SNMP (si no, hereda los 18 kg de la hoja)

    -- Sistema con monitor incluido dentro de 'PC'.
    ('INV', '0417811',  18000,  55,  60,  55),  -- Sist. Kelyx Ryzen 3 + Monitor 24"

    -- Dos productos mal categorizados que hoy cuelgan de Notebooks → Consumo y heredan sus 3 kg. Se
    -- les pone el peso real acá para frenar la pérdida; recategorizarlos es una decisión de catálogo
    -- y va aparte.
    ('INV', '0417336',  22000,  85,  40,  70),  -- Silla Gamer Raptor Throne R1 Tela Negra (¡en Notebooks!)
    ('INV', '0415685',   8000,  35,  50,  45)   -- Epson L8050, una impresora (¡en Notebooks!)
)
UPDATE productos p
SET peso_gramos = o.peso,
    alto_cm     = o.alto,
    ancho_cm    = o.ancho,
    largo_cm    = o.largo,
    updated_at  = now()
FROM overrides o
JOIN proveedores pv ON pv.codigo = o.proveedor_codigo
WHERE p.proveedor_id = pv.id
  AND p.codigo_externo = o.codigo_externo;
