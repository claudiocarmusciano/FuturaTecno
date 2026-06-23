# Deploy de FuturaTecno en Railway

App empaquetada como **una sola imagen Docker**: el frontend (React/Vite) se buildea
y queda servido por el backend (Spring Boot, mismo origen). PostgreSQL es un servicio
gestionado de Railway. El esquema lo crean las migraciones de **Flyway** al arrancar.

---

## 1. Crear el proyecto en Railway

1. Entrá a https://railway.app y logueate con GitHub.
2. **New Project → Deploy from GitHub repo →** elegí `FuturaTecno`.
3. Railway detecta el `Dockerfile` de la raíz y lo usa para buildear. No hace falta config extra.

## 2. Agregar PostgreSQL

1. En el proyecto: **New → Database → Add PostgreSQL**.
2. Railway crea un servicio `Postgres` con variables internas: `PGHOST`, `PGPORT`,
   `PGDATABASE`, `PGUSER`, `PGPASSWORD`.

## 3. Variables de entorno del servicio backend

En el servicio de la app (no el de Postgres) → pestaña **Variables** → agregá:

| Variable | Valor | Notas |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` | Referencia al servicio Postgres |
| `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` | |
| `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` | |
| `JWT_SECRET` | *(string aleatorio de 40+ caracteres)* | **Crítico.** `openssl rand -base64 48` |
| `ADMIN_EMAIL` | tu email de admin | Reemplaza el default |
| `ADMIN_PASSWORD` | *(contraseña fuerte)* | **Reemplaza `admin1234`** |
| `ANTHROPIC_API_KEY` | tu API key de Anthropic | Necesaria para parsing IA y búsqueda de imágenes |
| `CUTOFF_HOUR` | `14` | Opcional (hora de corte para la ETA) |

**Opcionales (imágenes vía Icecat):** `ICECAT_USERNAME`, `ICECAT_API_TOKEN`, `ICECAT_CONTENT_TOKEN`.
**Opcionales (Google Custom Search):** `GOOGLE_API_KEY`, `GOOGLE_CX`. Si no se setean, la
búsqueda de imágenes usa solo la API de Anthropic (recomendado).

> La sintaxis `${{Postgres.VARIABLE}}` es de Railway para referenciar variables de otro
> servicio. Ajustá el nombre `Postgres` si tu servicio se llama distinto.
>
> Railway inyecta `PORT` automáticamente — el backend ya lo lee (`server.port=${PORT:8080}`).
> **No** setees `PORT` a mano.

## 4. Exponer el dominio

1. En el servicio backend → **Settings → Networking → Generate Domain**.
2. Te da una URL `https://<algo>.up.railway.app` con HTTPS incluido.

## 5. Dominio propio (cuando lo compres)

1. Railway → servicio backend → **Settings → Networking → Custom Domain** → ingresá tu dominio
   (ej: `futuratecno.com.ar`).
2. Railway te da un valor **CNAME**. En tu proveedor de DNS (o Cloudflare) creá el registro
   CNAME apuntando ahí. (Para el dominio raíz/apex puede requerir un ALIAS/ANAME o usar Cloudflare).
3. El certificado SSL lo emite Railway automáticamente.

## 6. Primer arranque

- **Flyway** corre las migraciones `V1`…`V5` y crea todas las tablas en la base vacía.
- `AdminInitializer` crea el usuario admin con `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
- Entrá a la URL, logueate con esas credenciales.

## 7. Deploys siguientes

Cada `git push` a `main` dispara build + deploy automático. Nada más que hacer.

---

## Notas

- **Datos:** los productos/proveedores de tu base local NO se migran solos (es otra base).
  Cargás los proveedores y corrés el parsing de nuevo, o exportás/importás con `pg_dump`.
- **Backups:** verificá la política de snapshots de Postgres en tu plan de Railway. Para datos
  reales conviene un `pg_dump` periódico.
- **Costo:** plan Hobby de Railway ~USD 5/mes (app + Postgres comparten el crédito).
- **Secretos:** nunca subas el `.env` real (está en `.gitignore`). En Railway van como Variables.

## Probar la imagen localmente (opcional)

```bash
docker build -t futuratecno:test .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5433/futuratecno_db' \
  -e SPRING_DATASOURCE_USERNAME='futuratecno' \
  -e SPRING_DATASOURCE_PASSWORD='futuratecno' \
  -e JWT_SECRET='local-test-secret-de-al-menos-32-bytes-1234567890' \
  -e ADMIN_EMAIL='admin@futuratecno.com' \
  -e ADMIN_PASSWORD='admin1234' \
  -e ANTHROPIC_API_KEY='sk-ant-...' \
  futuratecno:test
# abrir http://localhost:8080  (frontend + API en el mismo origen)
```
