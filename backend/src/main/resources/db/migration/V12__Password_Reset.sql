-- "Olvidé mi contraseña": token de reseteo por email.
-- Se guarda el HASH del token (SHA-256), nunca el token en texto plano: si se filtra la base,
-- los hashes no sirven para resetear. El token real solo viaja en el email al usuario.
ALTER TABLE usuarios ADD COLUMN reset_token VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN reset_token_expira TIMESTAMP;

-- Búsqueda por token al validar el reseteo.
CREATE INDEX idx_usuarios_reset_token ON usuarios (reset_token);
