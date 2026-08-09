-- Verificación de WhatsApp, email y seguimiento de los pasos del sorteo de bienvenida.
ALTER TABLE usuarios ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN whatsapp_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN whatsapp_codigo_hash VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN whatsapp_codigo_expira TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN paso_whatsapp_agendado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN paso_instagram_completado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN email_activacion_token VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN email_activacion_expira TIMESTAMP;

-- Las cuentas existentes se crearon antes de este requisito y no deben quedar bloqueadas.
UPDATE usuarios SET email_verificado = TRUE, whatsapp_verificado = TRUE,
  paso_whatsapp_agendado = TRUE, paso_instagram_completado = TRUE;

CREATE INDEX idx_usuarios_email_activacion ON usuarios(email_activacion_token);
