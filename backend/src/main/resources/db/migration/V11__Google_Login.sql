-- Login con Google (Sign in with Google).
-- Los usuarios que entran con Google no tienen contraseña local: la columna pasa a ser opcional.
ALTER TABLE usuarios ALTER COLUMN password DROP NOT NULL;

-- Identificador estable de la cuenta de Google: el claim "sub" del ID token (no cambia nunca,
-- a diferencia del email). Sirve para reconocer la cuenta aunque el usuario cambie su email.
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255);

-- Único cuando está presente. En Postgres los NULL se consideran distintos entre sí,
-- así que las cuentas sin Google (google_sub NULL) no chocan con este índice.
CREATE UNIQUE INDEX ux_usuarios_google_sub ON usuarios (google_sub);
