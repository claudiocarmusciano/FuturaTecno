# FuturaTecno — Cheat Sheet

## Stack
- **Backend:** Java 21 + Spring Boot 3.3 + Spring Security + JWT + JPA/Hibernate + Flyway + PostgreSQL 16
- **Frontend:** React 18 + Vite + React Router + axios + Context auth | **JSX (no TypeScript)**
- **AI:** Claude Haiku 4.5 (`claude-haiku-4-5-20251001`) — clasificación de categorías + búsqueda de imágenes. **Opcional:** sin `ANTHROPIC_API_KEY` las dos degradan solas (la clasificación deja `categoria_id` en null para corregir a mano en Admin → Productos; las imágenes caen a Icecat). El parsing de listas de precios con IA **se eliminó** (2026-07-31): lo reemplazó "Cargar por JSON", que no consume API.
- **Exchange rate:** dolarapi.com dólar oficial (cacheado en memoria)
- **Deploy:** Railway, push a `main` = auto-deploy (~2-3 min) | URL: `https://futuratecno-production.up.railway.app`

## Arquitectura no obvia
- **Frontend embebido en el jar:** Vite buildea a `backend/src/main/resources/static/` → mismo origen que el backend → sin CORS. El `Dockerfile` hace el build del frontend primero.
- **Sin Lombok:** se rompió bajo Java 25 durante el desarrollo → getters/setters explícitos en todas las entidades. No agregar Lombok.
- **Flyway activo (`ddl-auto=validate`):** las migraciones van en `backend/src/main/resources/db/migration/` (ya hay V1–V6). Cualquier cambio de schema necesita un archivo `V7__...sql` nuevo. **No cambiar a `ddl-auto=update`.**
- **Fórmula de precios:** `(costo USD) × (1 + flete%) × (1 + margen%)` — el flete es **porcentaje por proveedor**, no monto fijo. Defaults: flete 5%, margen 15%.
- **Soft delete:** `activo=false` en `Producto` y `Variante`. Nunca borrar físicamente.
- **Imagen por PRODUCTO** (no por variante): `Producto.imagenUrl`. Una sola imagen por producto independientemente de cuántas variantes tenga.
- **MercadoLibre bloquea scraping server-side** → cualquier request no-browser devuelve 302 a "suspicious-traffic". Las imágenes ML no se pueden auto-obtener. Flujo manual: Admin → Imágenes → "🔍 Buscar" abre Google Images → el usuario copia la URL de la imagen → la pega → preview → Guardar.
- **Sincronización automática diaria:** `SincronizacionScheduler` a las 06:30 AR (configurable con `SYNC_CRON`). Modo "solo existentes": actualiza precio/stock de productos ya importados, no crea nuevos.
- **Mayoristas modulares:** cada distribuidor tiene su propio `ApiClient` + `ImportService` + `Controller`. Elit y Invid conviven sin pisarse. Dedup por `codigo_externo` + `fuente`.
- **Railway bloquea la salida SMTP** (puertos 25/465/587, política antispam) → los mails **no** pueden ir por SMTP. Un intento contra `smtp.gmail.com` no falla con error de credenciales: cuelga y muere en `ConnectException: Connection timed out`, porque nunca llega a autenticar. Por eso `EmailService` manda por la **API HTTP de Resend** (443). Cualquier integración saliente nueva: verificar que no dependa de un puerto no-HTTP.
- **Los mails van asíncronos y con timeout.** `EmailService#enviarHtmlAsync` (`@EnableAsync`) — el mail es un efecto secundario y no debe demorar ni tumbar la operación que lo dispara. Sin timeout explícito, un SMTP/HTTP colgado se queda con un hilo de Tomcat indefinidamente (pasó: un `forgot-password` tardaba ~5 min).
- **Pedidos: los precios se guardan CONGELADOS.** El precio de venta es derivado y se recalcula todos los días (cotización + costos del mayorista), así que `pedido_items` copia nombre, SKU, especificaciones e importes al confirmar. `producto_id`/`variante_id` quedan solo para trazabilidad — **nunca** mostrar un pedido leyendo el producto vivo, mostraría precios de hoy y no los que el cliente aceptó.
- **El backend recalcula el precio del pedido.** El carrito manda solo `varianteId` + `cantidad`. Si se confiara en un importe del cliente, cualquiera pediría un iPhone a un dólar.
- **Los pedidos vencen a las 06:30 AR** (`PedidoScheduler`), que es cuando la sync pisa precios y stock. Es un scheduler **aparte** del de sincronización a propósito: apagar `SYNC_ENABLED` no debe dejar pedidos vivos con precios viejos.
- **No se descuenta stock al pedir.** `Variante.stock` lo pisa la sync diaria, así que cualquier descuento local se perdería a la mañana. El pedido es una solicitud, no una reserva.
- **El carrito no requiere cuenta** (vive en localStorage); la sesión se pide recién al confirmar. Al abrir `/carrito` se revalidan los precios contra la API.
- **Ojo con `SecurityConfig`:** la cadena termina en `anyRequest().permitAll()`. Toda ruta privada nueva hay que listarla explícitamente (como `/api/pedidos/**`), o nace pública.
- **Confirmar un pedido exige `aceptaCompromiso: true` en el body.** Se valida en `PedidoService.crear()`, no solo en el checkbox del frontend — mismo criterio que el precio: no confiar en lo que manda el cliente.
- **Peso/dimensiones para envío (V14):** `Producto` tiene `pesoGramos`/`altoCm`/`anchoCm`/`largoCm` opcionales (override real, se edita en Admin → Productos). Si están en `null`, hay que resolver por el default de `Categoria` (mismos 4 campos con sufijo `_default`): primero la subcategoría (hoja), si no tiene, la categoría padre. Ningún mayorista (Elit/Invid) ni Icecat con el plan actual traen este dato — confirmado pegándole a la API de Icecat en vivo, el plan da `GeneralInfo`/`Gallery` pero no `FeaturesGroups` (ahí vive el peso). Los defaults por categoría se cargaron a mano en la V14 y solo se editan por SQL directo, no hay UI para eso todavía.
- **Envío por Andreani (V15):** la cotización sale de la **API Pyme** (`woocommerce-api-acom.andreani.com`), que es el middleware del plugin de WooCommerce — **no** la API corporativa (`apis.andreani.com`, esa exige contrato comercial y nunca la conseguimos). Es la única vía para una cuenta Pyme, pero Andreani podría cambiarla sin avisar: por eso `AndreaniClient` degrada con gracia y el checkout funciona igual sin cotización. Auth: la credencial del portal (Integraciones → WooCommerce) va tal cual en `Authorization` a `POST /api/v1/Login`; la respuesta trae el `accessToken` (header `X-Auth-Token` de ahí en más) **y los contratos de la cuenta** — no hace falta conocer `cliente` ni `contrato`, los devuelve la API. Cotización: `POST /api/v1/Pyme/rates` con `{postal_code_origin, postal_code_destination, products[{quantity, price, dimensions{width,height,depth,grams}}]}`. Exige los 4 datos de peso/dimensiones de cada producto (de ahí la V14); si falta alguno no se cotiza. La modalidad "sucursal" viene repetida una vez por punto de retiro del CP → hay que quedarse con el mínimo por modalidad.
- **El costo de envío también se congela.** Mismo criterio que los precios: el checkout manda solo CP + modalidad, `PedidoService` **recotiza server-side** y guarda el importe. Si Andreani no responde en ese momento, el pedido igual se crea con `costo_envio_ars` en null ("a cotizar") — la cotización nunca bloquea una venta.
- **Repo público en GitHub** — NUNCA commitear secrets. Las credenciales van solo en `backend/.env` (gitignored) y en Railway.

## Límites conocidos (escalabilidad)
Medido el 2026-07-31 con **1.545 productos activos**. Nada de esto está roto hoy, pero son los techos que hay que mirar cuando el catálogo crezca.
- **N+1 del catálogo — resuelto (2026-08-02).** `CatalogoService` y `ProductoAdminService` hacían una query de variantes (y el catálogo, además, de imágenes) **por producto**: con 1.545 productos, ~3.100 queries por request y `GET /api/productos` en 9-14 s. Se cambió a `findByProductoIdInAndActivo` / `findByProductoIdInAndActivoOrderByOrden` (una sola consulta con `IN` para todo el catálogo) + agrupar por producto en memoria (`Collectors.groupingBy`). Bajó a un puñado de queries totales, sin importar cuántos productos haya. Si en algún momento vuelve a sentirse lento, revisar que ningún código nuevo haya vuelto a meter una consulta dentro del loop de productos.
- **La paginación del catálogo es falsa.** `GET /api/productos` **acepta pero ignora** `page` y `size`: siempre devuelve todo (1 MB, 162 KB con gzip) y el frontend pagina de a 24 en el navegador. Ojo al medir: pedir páginas en un bucle devuelve la misma lista repetida (así conté 36.783 productos que en realidad eran 1.545 × 24).
- **El buscador del admin filtra en el cliente** (`ProductosPage`), sobre el listado completo que ya viene cargado. A este volumen es instantáneo; si el catálogo crece un orden de magnitud hay que pasarlo a server-side junto con la paginación.
- **Clasificación de categorías sin IA (`ClasificadorPorNombre`).** El vocabulario de categorías de Elit no calza con el árbol, y como el último recurso del clasificador era Claude (sin crédito), el 60% del catálogo quedaba con `categoria_id` en null — y sin categoría no se resuelve el peso, así que esos productos **no cotizan envío** (peor: `EnvioService` corta la cotización del **carrito entero** si un solo ítem no resuelve medidas). Se resuelve por reglas sobre el nombre, antes de la IA. **Clave: mira solo las primeras 4 palabras del modelo**, donde el mayorista pone el tipo de producto; el resto son especificaciones que mienten ("Procesador ... con Cooler" no es un cooler). Medido: leer el nombre entero da 31% de errores, leer el encabezado lo baja a 7%. La subcategoría sí usa el texto completo (chipset, Hz, si un switch es administrable). Ante la duda devuelve null a propósito: mal categorizado hereda un peso equivocado y cotiza mal sin que nadie se entere. Hay un test contra un volcado del catálogo real: `mvn test -Dtest=ClasificadorPorNombreCatalogoRealTest -Dcatalogo=/ruta/catalogo.json`.

## Convenciones
- **Auth:** catálogo público sin auth. Solo `/api/admin/**` requiere rol ADMIN. `/api/auth/register` crea rol USUARIO (sin acceso admin).
- **Admin único:** creado desde env vars `ADMIN_EMAIL` / `ADMIN_PASSWORD` por `AdminInitializer` al arrancar.
- **Precios mostrados:** USD + ARS (cotización en vivo). El catálogo público oculta proveedor y stock real ("Stock sujeto a disponibilidad").
- **ETA:** 3 días hábiles, cutoff 14:00 AR, saltea fines de semana + feriados argentinos (API nager.date).
- **Colores de marca (no hardcodear):** lime `#C8E048` (acento, texto oscuro encima), deep lime `#5D6B14` (texto/links sobre fondo claro), dark `#16181d` (header público + sidebar admin). El logo necesita fondo oscuro (wordmark "Futura" es blanco).
- **Responsive obligatorio:** sidebar admin → hamburger drawer en mobile. Tablas con scroll horizontal.

## Dev local
```bash
# 1. Levantar Postgres (puerto 5433, no 5432)
docker-compose up -d postgres

# 2. Backend — secrets en backend/.env (gitignored, nunca en application.yml)
cd backend && mvn spring-boot:run   # → http://localhost:8080

# 3. Frontend — proxea /api → :8080
cd frontend && npm run dev          # → http://localhost:5173
```

**Credenciales locales** (definidas en `backend/.env`):
- DB: `jdbc:postgresql://localhost:5433/futuratecno_db` | user: `futuratecno` | pass: `futuratecno`
- Admin: definido en `.env` con `ADMIN_EMAIL` / `ADMIN_PASSWORD`

## Prod (Railway)
| Env var | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | PostgreSQL managed (usar variables de Railway `${{Postgres.*}}`) |
| `JWT_SECRET` | Clave JWT |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin inicial |
| `ANTHROPIC_API_KEY` | Claude API para clasificar categorías y buscar imágenes. Vacía = las dos degradan solas, el resto del sistema anda igual. |
| `ANTHROPIC_MODEL` | Modelo IA (default: `claude-haiku-4-5-20251001`) |
| `SYNC_ENABLED` / `SYNC_CRON` | Sync automática (default: 06:30 AR) |
| `ELIT_USER_ID` / `ELIT_TOKEN` | Credenciales mayorista Elit |
| `INVID_BASE_URL` | Host de Invid/TornadoStore |
| `GOOGLE_CLIENT_ID` | Client ID de "Sign in with Google" (OAuth Web, no secreto). Vacío = botón oculto. Lo sirve `GET /api/config` al frontend y lo usa `GoogleTokenVerifier` para validar el ID token. |
| `RESEND_API_KEY` | API key de Resend (`re_...`) para mandar mails. Vacío = no se manda nada (el flujo responde igual, queda en el log). |
| `MAIL_FROM` / `MAIL_FROM_NAME` | Remitente. **Tiene que ser del dominio verificado en Resend** (`no-responder@futuratecno.com.ar`); un Gmail se rechaza por dominio no verificado. |
| `ADMIN_NOTIFY_EMAIL` | Destinatario de avisos internos. Si no está, usa `ADMIN_EMAIL`. |
| `ANDREANI_HASH` | Credencial de la cuenta Pyme de Andreani (portal → Integraciones → WooCommerce). Vacía = sin cotización de envío (el checkout sigue andando). |
| `ANDREANI_CP_ORIGEN` | Código postal de despacho (origen de toda cotización). |

**Dominio:**
- `futuratecno.com.ar` — **registrado** en NIC.ar (dominio "especial" pago; vence 2027-07-16). Falta **delegar el DNS** (todavía sin nameservers) para apuntarlo a Railway.
- `futuratecno.com` — **NO es nuestro**: pertenece a un tercero (registrado en 2018 vía Bluehost, apuntando a Wix). No usar ni asumir disponibilidad.
