# Cargar desde Notion

En Admin → Cargar por JSON → Texto del proveedor, **Cargar desde Notion** lee la página configurada, copia su texto al formulario y ejecuta la generación y búsqueda de imágenes existentes. No exige fecha ni proveedor en Notion. Elegir el proveedor en el administrador y pulsar **Confirmar e importar** sigue siendo necesario para modificar el catálogo.

El texto y los artículos quedan en memoria en la pantalla: editar o borrar la fuente después de leerla no los cambia, pero cerrar o recargar la pantalla pierde el borrador. Descargar JSON o importar antes de salir. Una nueva carga reemplaza el borrador anterior. La app nunca borra ni escribe en Notion.

## Configuración del servidor

1. Crear una conexión interna de Notion con capacidad de lectura de contenido y concederle acceso a la página usada para los listados.
2. Guardar su token como `NOTION_TOKEN` en `backend/.env` para desarrollo. No pegarlo en el chat ni incluirlo en archivos versionados.
3. Configurar `NOTION_PAGE_ID` con el ID de la página definitiva (nombre previsto: “Listado para JSON”). El título no interviene en la conexión: renombrar la misma página conserva el ID; crear otra página requiere configurar su nuevo ID y darle acceso a la conexión. La página “Proveedor Dropshipping” fue solo un ejemplo.
4. Reiniciar el backend y comprobar una carga desde el administrador.

El conector de Notion del chat no configura ni autoriza al backend. Para usarlo con la Mac apagada, configurar también las variables en el servidor y desplegar los cambios, únicamente cuando el usuario autorice publicar.

Se leen bloques de texto, encabezados, listas, tablas y texto anidado, recorriendo la paginación. No se leen adjuntos ni texto dentro de imágenes. Subpáginas y otros bloques no compatibles provocan un aviso, evitando importar parcialmente el listado. Límite compartido con el generador: 20.000 caracteres; páginas demasiado complejas se rechazan. Errores de permisos, cuota o conexión no importan artículos.

Referencias oficiales: https://developers.notion.com/reference/authentication y https://developers.notion.com/reference/get-block-children

## Reglas del listado

Para la misma marca y denominación se conserva un artículo con el precio USD más alto. Las variantes reales (RAM, capacidad, color y combo) siguen separadas. Se excluyen artículos con fallas o condiciones especiales, incluidos usados, reacondicionados, open box, sin caja o garantía, dañados y para repuestos. Ofertas y liquidaciones por sí solas no se excluyen. Las omisiones y unificaciones aparecen en los avisos de revisión. Estas reglas se aplican a nuevas generaciones, tanto desde texto como desde Notion; no modifican productos ya importados ni JSON pegados manualmente.
