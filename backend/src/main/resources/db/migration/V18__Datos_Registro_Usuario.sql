-- Datos de contacto para las cuentas creadas desde el registro tradicional.
-- Se mantienen nullable para conservar compatibilidad con usuarios existentes y acceso por Google.
ALTER TABLE usuarios ADD COLUMN apellido VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN celular VARCHAR(20);
