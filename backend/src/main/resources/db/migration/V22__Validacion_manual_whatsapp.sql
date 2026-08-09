-- Código visible que el usuario envía por WhatsApp y el administrador valida manualmente.
ALTER TABLE usuarios ADD COLUMN whatsapp_verificacion_codigo VARCHAR(20);

CREATE UNIQUE INDEX idx_usuarios_whatsapp_verificacion_codigo
    ON usuarios(whatsapp_verificacion_codigo)
    WHERE whatsapp_verificacion_codigo IS NOT NULL;
