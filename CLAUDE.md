# FuturaTecno — Cheat Sheet

## Stack
- **Backend:** Java 21 + Spring Boot 3.3 + Spring Security + JWT + JPA/Hibernate + Flyway + PostgreSQL 16
- **Frontend:** React 18 + Vite + React Router + axios + Context auth | **JSX (no TypeScript)**
- **AI:** Claude Haiku 4.5 (`claude-haiku-4-5-20251001`) — parsing de listas de precios + búsqueda de imágenes
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
- **Repo público en GitHub** — NUNCA commitear secrets. Las credenciales van solo en `backend/.env` (gitignored) y en Railway.

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
| `ANTHROPIC_API_KEY` | Claude API para parsing e imágenes |
| `ANTHROPIC_MODEL` | Modelo IA (default: `claude-haiku-4-5-20251001`) |
| `SYNC_ENABLED` / `SYNC_CRON` | Sync automática (default: 06:30 AR) |
| `ELIT_USER_ID` / `ELIT_TOKEN` | Credenciales mayorista Elit |
| `INVID_BASE_URL` | Host de Invid/TornadoStore |
| `GOOGLE_CLIENT_ID` | Client ID de "Sign in with Google" (OAuth Web, no secreto). Vacío = botón oculto. Lo sirve `GET /api/config` al frontend y lo usa `GoogleTokenVerifier` para validar el ID token. |

**Dominio:**
- `futuratecno.com.ar` — **registrado en NIC.ar** (desde 2026-07-16, vence 2027-07-16). Falta **delegar el DNS** en NIC.ar para apuntarlo a Railway.
- `futuratecno.com` — **NO es nuestro**: pertenece a un tercero (registrado en 2018 vía Bluehost, apuntando a Wix). No usar ni asumir disponibilidad.
