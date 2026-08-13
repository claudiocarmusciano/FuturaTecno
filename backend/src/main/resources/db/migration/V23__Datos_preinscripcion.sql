-- Datos requeridos para la preinscripción y control de participación única en el sorteo.
ALTER TABLE usuarios ADD COLUMN dni VARCHAR(8);
ALTER TABLE usuarios ADD COLUMN fecha_nacimiento DATE;

CREATE UNIQUE INDEX idx_usuarios_dni_unico
    ON usuarios(dni)
    WHERE dni IS NOT NULL;
