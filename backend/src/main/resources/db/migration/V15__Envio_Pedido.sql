-- Envío por Andreani en los pedidos (elegido en el checkout, opcional).
--
-- Mismo criterio que los importes de los ítems: el costo se CONGELA al confirmar, recotizado
-- server-side contra Andreani en ese momento — nunca el número que mande el cliente. Los tres
-- campos son nullables: un pedido sin envío (retiro / a coordinar) queda todo en null, y si
-- Andreani no responde al confirmar, queda la modalidad elegida con costo null ("a cotizar").
--
-- modo_envio guarda el código tal como lo da Andreani ("estándar", "sucursal", "llega hoy", ...);
-- no es un enum nuestro a propósito: las modalidades son de ellos y pueden aparecer nuevas.

ALTER TABLE pedidos ADD COLUMN cp_destino VARCHAR(10);
ALTER TABLE pedidos ADD COLUMN modo_envio VARCHAR(30);
ALTER TABLE pedidos ADD COLUMN costo_envio_ars NUMERIC(14, 2);
