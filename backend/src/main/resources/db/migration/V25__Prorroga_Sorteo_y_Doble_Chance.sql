-- Prórroga comunicada el 31/08/2026. Las cuentas de clientes creadas hasta ese día
-- conservan una participación adicional, incluso si completan las validaciones después.
ALTER TABLE usuarios ADD COLUMN chances_sorteo INTEGER NOT NULL DEFAULT 1;
ALTER TABLE usuarios ADD COLUMN aviso_prorroga_enviado_en TIMESTAMP;

UPDATE usuarios
SET chances_sorteo = 2
WHERE rol = 'USUARIO'
  AND created_at < TIMESTAMP '2026-09-01 00:00:00';
