-- 2026-11-16, pedido del usuario: sacar de la tienda lo que ExclusionesCarga#sinCajaOCajaDanada ya no
-- deja cargar. Se desactivan (no se borran): los pedidos viejos siguen apuntando a su producto.
UPDATE productos SET activo = false
WHERE activo AND translate(upper(modelo), 'ÁÉÍÓÚÑ', 'AEIOUN') ~
    '(\mSIN CAJA\M|\mS/ ?CAJA\M|\mCAJA (DANADA|ROTA|GOLPEADA|AVERIADA|ABOLLADA|MALTRATADA|CON (DETALLES?|DANOS?|GOLPES?))\M|\mDANOS? EN (LA )?CAJA\M|\m(DAMAGED|BROKEN) BOX\M|\mBOX DAMAGED\M|\mNO BOX\M|\mWITHOUT BOX\M)';
