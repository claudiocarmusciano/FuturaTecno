-- "Depurar catálogo" filtraba por `updated_at`, asumiendo que un producto que no se actualiza hace
-- N días es uno que el mayorista dejó de ofrecer. La premisa era falsa.
--
-- Medido el 2026-09-16: la importación de Invid informó "1.188 actualizados" y solo 70 productos
-- quedaron con la fecha de ese día. Los otros 1.134 siguen en el feed de Invid, pero como su precio
-- y su stock vinieron idénticos, Hibernate no emitió ningún UPDATE y `updated_at` no se movió. Dos
-- días después habrían aparecido como "vencidos" con la sincronización funcionando perfecto, y
-- darles de baja habría sacado de la tienda un tercio del catálogo.
--
-- `updated_at` responde "¿cambió algo?". La pregunta de esa pantalla es otra: "¿el mayorista lo
-- sigue teniendo?". Esta columna responde esa: la escriben los imports de Elit e Invid para CADA
-- producto que encuentran en el feed, cambie o no su precio.
--
-- Se escribe con un UPDATE masivo por ids justamente para NO pasar por @PreUpdate: si tocara
-- `updated_at`, el catálogo pasaría a decir "Actualizado: hoy" en todo, y se perdería el único
-- dato que indica cuándo cambió el precio de verdad.
--
-- Queda en NULL para lo cargado por JSON (Cozzo, Kadmiel): ahí no hay feed que mire nadie y
-- `updated_at` sí es la señal honesta, porque refleja cuándo recargaste vos el listado.
ALTER TABLE productos ADD COLUMN visto_en_sync_at TIMESTAMP;

CREATE INDEX idx_productos_visto_en_sync ON productos (visto_en_sync_at);
