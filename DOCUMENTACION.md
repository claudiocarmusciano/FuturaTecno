# 📘 Documentación — FuturaTecno

Plataforma de venta de tecnología (notebooks y afines) para **Olavarría, Buenos Aires**.
Los proveedores (en CABA) mandan listas de precios por WhatsApp en texto libre; el sistema
las **parsea automáticamente con IA (Claude)**, calcula el precio de venta y las publica en
un catálogo online con cotización del dólar en vivo.

- **App en vivo:** https://futuratecno-production.up.railway.app
- **Repositorio:** https://github.com/claudiocarmusciano/FuturaTecno (público)
- **Dominio propio:** `futuratecno.com.ar` (registro pendiente de autorización en NIC.ar)

---

## 1. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.3 · Spring Security · JPA/Hibernate |
| Base de datos | PostgreSQL 16 · migraciones con **Flyway** (V1–V5) |
| Frontend | React 18 · Vite · React Router v6 · Axios |
| Autenticación | JWT (jjwt) · BCrypt |
| IA | Anthropic Claude **Haiku 4.5** (`claude-haiku-4-5-20251001`) — parsing **e** imágenes |
| Cotización USD/ARS | API pública de **dolarapi.com** (dólar oficial) |
| Feriados | API pública de **nager.date** (feriados de Argentina) |
| Empaquetado | Docker (imagen única) |
| Hosting | **Railway** (app + PostgreSQL gestionado) |

> Nota: no se usa Lombok (rompía bajo Java 25); las entidades tienen getters/setters explícitos.

---

## 2. Arquitectura

**Imagen Docker única**: el frontend (React/Vite) se compila y se **embebe dentro del JAR** de
Spring Boot como recursos estáticos (`src/main/resources/static/`). El backend sirve tanto la
API como la SPA → **mismo origen, sin CORS**.

```
Navegador
   │
   ▼
Spring Boot (puerto 8080 / $PORT)
   ├── /api/**          → controllers REST (JSON)
   └── /** (resto)      → index.html + assets de React (SPA, deep-links vía SpaConfig)
   │
   ▼
PostgreSQL (Flyway corre V1..V5 al arrancar)
```

- Las rutas que **no** empiezan con `api/` y no son un archivo estático devuelven `index.html`
  (para que el ruteo de React Router funcione en links directos, ej. `/producto/5`).
- `AdminInitializer` crea el usuario ADMIN al primer arranque si no existe.

---

## 3. Funcionalidades

### Público (sin login)
- **Catálogo** con tarjetas de producto (imagen, marca/modelo, precio USD + ARS).
- **Filtros dinámicos**: búsqueda por texto, categoría, marca, rango de precio (US$), orden por precio.
- **Cotización del dólar** mostrada en vivo.
- **Estimación de entrega (ETA)**: "tu pedido llega aprox. el …".
- **Página de detalle** del producto con specs, precio, ETA y **botón de WhatsApp** (abre el chat con el producto ya identificado).
- Aclaraciones: "Stock sujeto a disponibilidad" y "las imágenes son meramente ilustrativas".

### Panel Admin (`/admin`, requiere rol ADMIN)
- **Dashboard** con métricas (productos publicados, clientes registrados, proveedores, dólar) y últimos registrados.
- **Proveedores**: CRUD (nombre, % margen, % flete). Borrado lógico + reactivación por nombre.
- **Parsing IA**: pegás el texto de WhatsApp → la IA arma un **preview** de productos/variantes/precios → confirmás → se importan (upsert).
- **Productos**: listar, **editar** (categoría, marca, modelo, specs, costo, moneda, stock) y eliminar.
- **Imágenes**: búsqueda automática + carga manual de URL con preview (ver §8).
- **Usuarios**: base de mails de clientes registrados + exportación CSV.

---

## 4. Modelo de datos

Migraciones en `backend/src/main/resources/db/migration/`. Todas las tablas tienen
`activo` (borrado lógico), `created_at`, `updated_at`.

### `proveedores`
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| nombre | VARCHAR(255) **UNIQUE** | |
| margen_porcentaje | NUMERIC(5,2) | % de ganancia |
| flete_porcentaje | NUMERIC(5,2) | % de flete (antes era monto fijo USD — ver V4) |
| activo, created_at, updated_at | | |

### `productos`
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| proveedor_id | BIGINT FK → proveedores | |
| categoria | VARCHAR(255) | ej. "Notebook gamer" (V2) |
| marca | VARCHAR(255) | |
| modelo | VARCHAR(255) | |
| imagen_url | VARCHAR(1000) | imagen principal (V3) |
| activo, created_at, updated_at | | |

### `variantes`
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| producto_id | BIGINT FK → productos | |
| especificaciones | VARCHAR(500) | texto libre, ej. "RYZEN 5 / 8GB / 512SSD" — clave para actualizar precios (V2) |
| almacenamiento_gb | INTEGER | |
| ram_gb | INTEGER | |
| color | VARCHAR(100) | |
| costo_usd | NUMERIC(10,2) | costo canónico en USD |
| moneda_origen | VARCHAR(3) | USD/ARS — moneda original del proveedor (V2) |
| precio_origen | NUMERIC(14,2) | monto original (V2) |
| stock | INTEGER | |
| activo, created_at, updated_at | | |

### `imagenes`
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| variante_id | BIGINT FK → variantes | |
| url | VARCHAR(500) | |
| orden | INTEGER | |

### `usuarios` (V5)
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| email | VARCHAR(255) **UNIQUE** | |
| password | VARCHAR(255) | hash BCrypt |
| nombre | VARCHAR(255) | |
| rol | VARCHAR(20) | `ADMIN` o `USUARIO` (default USUARIO) |
| activo, created_at, updated_at | | |

**Relaciones:** `proveedor 1—N producto 1—N variante 1—N imagen`.

---

## 5. Lógica de negocio

### Cálculo del precio de venta (`CatalogoService`)
```
precioVentaUsd = costoUsd × (1 + flete% / 100) × (1 + margen% / 100)
precioVentaArs = precioVentaUsd × cotizaciónDólarOficial
```
- `flete%` y `margen%` se definen **por proveedor**.
- El admin carga el **costo**; la venta se calcula sola.

### Cotización del dólar
- Se obtiene de **dolarapi.com** (dólar oficial) y se cachea ~30 min.

### ETA (estimación de entrega) — `EtaService` + `FeriadosService`
- **3 días hábiles**, salteando **fines de semana** y **feriados de Argentina** (nager.date).
- Respeta una **hora de corte** diaria (`CUTOFF_HOUR`, default 14:00): comprado después del corte, cuenta desde el día siguiente.

### Borrado lógico
- Nada se borra físicamente: se marca `activo = false`. Los proveedores inactivos se **reactivan** si se vuelve a crear uno con el mismo nombre.

---

## 6. Roles y seguridad

- **Spring Security 6 + JWT**, stateless. Token en `localStorage`, enviado en `Authorization: Bearer <token>`.
- **Roles:**
  - `ADMIN` → único, creado al arrancar desde `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Accede a `/admin` y a `/api/admin/**`.
  - `USUARIO` → los que se registran. Quedan en la base de mails; **no** acceden al panel.
- **Reglas:** `/api/admin/**` requiere ADMIN; **todo lo demás es público** (el catálogo es abierto).
- Contraseñas hasheadas con **BCrypt**. Vencimiento del token: `JWT_EXPIRATION_MS` (default 7 días).

---

## 7. API REST

Base: `https://futuratecno-production.up.railway.app`. Todos los paths cuelgan de `/api`.

### Públicos
| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/health` | Healthcheck (lo usa Railway). |
| GET | `/api/productos` | Catálogo público (con precios de venta calculados). |
| GET | `/api/productos/{id}` | Detalle de un producto. |
| GET | `/api/cotizacion` | Cotización del dólar oficial. |
| GET | `/api/eta` | Fecha estimada de entrega. |
| POST | `/api/auth/register` | Registro de cliente (rol USUARIO). |
| POST | `/api/auth/login` | Login → devuelve JWT + rol. |

### Admin (requieren `Authorization: Bearer <token>` de un ADMIN)
| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/admin/productos` | Lista de productos (vista admin). |
| GET | `/api/admin/productos/{id}/editar` | Datos para editar un producto. |
| PUT | `/api/admin/productos/{id}` | Actualizar producto. |
| PUT | `/api/admin/productos/{id}/imagen` | Setear/editar URL de imagen. |
| POST | `/api/admin/productos/buscar-imagenes` | Búsqueda automática de imágenes faltantes. |
| DELETE | `/api/admin/productos/{id}` | Eliminar (lógico). |
| GET / POST / PUT / DELETE | `/api/admin/proveedores[/{id}]` | CRUD de proveedores. |
| POST | `/api/admin/parsing` | Parsea texto de WhatsApp → preview. |
| POST | `/api/admin/parsing/confirmar` | Confirma e importa el preview. |
| GET | `/api/admin/usuarios` | Base de clientes registrados. |

---

## 8. Búsqueda de imágenes

La imagen se guarda **por producto** (`productos.imagen_url`).

### Automática (`AnthropicImageService`)
1. Claude (herramienta `web_search`, hasta **2 búsquedas/producto**) busca la **ficha del producto**.
   El query incluye **categoría + marca + modelo + especificaciones** de la 1ª variante.
2. Prioriza **sitio oficial de la marca** + **tiendas argentinas** que no bloquean
   (Fravega, Cetrogar, Megatone, Compra Gamer, etc.). **Excluye MercadoLibre, Amazon y eBay.**
3. Descarga esa página y extrae el meta `og:image` (descartando logos/placeholders).

> ⚠️ **MercadoLibre bloquea el scraping desde servidor** (responde 302 a una página de
> "tráfico sospechoso", o 403). Su API también exige OAuth (403). Por eso la búsqueda
> automática **no** usa ML. Las imágenes del CDN de ML (`mlstatic.com`) sí son públicas si
> ya tenés la URL.

### Manual (la más confiable) — Admin → Imágenes
1. Botón **🔍 Buscar** abre Google Imágenes con marca+modelo+specs.
2. Click derecho en la foto → **Copiar dirección de imagen** → pegás la URL en la fila.
3. **Preview en vivo** de la imagen → **Guardar**.

---

## 9. Parsing IA (flujo)

1. Admin pega el texto crudo de WhatsApp en **Parsing IA** → `POST /api/admin/parsing`.
2. Claude estructura el texto en categoría/marca/modelo/variantes/precio/moneda → se devuelve un **preview** editable.
3. Admin confirma → `POST /api/admin/parsing/confirmar` → se importan (upsert por proveedor + specs; actualiza precios de variantes existentes).

---

## 10. Frontend — rutas

| Ruta | Página | Acceso |
|---|---|---|
| `/` | Catálogo | Público |
| `/producto/:id` | Detalle de producto | Público |
| `/login` | Iniciar sesión | Público |
| `/registro` | Crear cuenta | Público |
| `/admin` | Dashboard | ADMIN |
| `/admin/proveedores` | Proveedores | ADMIN |
| `/admin/parsing` | Parsing IA | ADMIN |
| `/admin/productos` | Productos | ADMIN |
| `/admin/imagenes` | Gestión de imágenes | ADMIN |
| `/admin/usuarios` | Usuarios / base de mails | ADMIN |

Config del frontend en `frontend/src/config.js`:
- `WHATSAPP_NUMBER = '5492284622222'` (número del negocio, con código de país, sin `+`).
- `NOMBRE_NEGOCIO = 'FuturaTecno'`.

**Responsive:** el panel admin usa un **menú hamburguesa (drawer)** en celular, las tablas
scrollean horizontalmente, el header escala el logo y el catálogo pasa a 1 columna.

---

## 11. Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/futuratecno_db` | Conexión a Postgres |
| `SPRING_DATASOURCE_USERNAME` | `futuratecno` | |
| `SPRING_DATASOURCE_PASSWORD` | `futuratecno` | |
| `PORT` | `8080` | Lo inyecta Railway |
| `ANTHROPIC_API_KEY` | *(vacío)* | **Obligatoria** — parsing + imágenes |
| `ANTHROPIC_MODEL` | `claude-haiku-4-5-20251001` | Modelo de Claude |
| `JWT_SECRET` | *(placeholder)* | **Cambiar en prod** (≥32 chars) |
| `JWT_EXPIRATION_MS` | `604800000` (7 días) | Vencimiento del token |
| `ADMIN_EMAIL` | `admin@futuratecno.com` | Email del admin inicial |
| `ADMIN_PASSWORD` | `admin1234` | **Cambiar en prod** |
| `CUTOFF_HOUR` | `14` | Hora de corte para la ETA |
| `ICECAT_*`, `GOOGLE_API_KEY`, `GOOGLE_CX` | *(vacío)* | Fuentes de imagen opcionales (no usadas) |

> Los secretos reales viven solo en `backend/.env` (gitignored). En Railway van como Variables.
> En `application.yml` solo hay **placeholders**.

---

## 12. Deploy (Railway)

- **Push a `main` = build + deploy automático.** Build: compila el frontend (Vite) y el backend (Maven) en una sola imagen Docker.
- PostgreSQL es un servicio gestionado llamado **`Postgres`**; las variables de DB se referencian con `${{Postgres.PGHOST}}`, etc.
- Healthcheck en `/api/health`.
- **La base de producción es independiente de la local** — el catálogo se carga en producción desde `/admin`.

**Dominio propio:** `futuratecno.com.ar` (NIC.ar) está en *"Pendiente Validación"* porque cayó
en la cola de **dominios especiales (banquinado)** que requiere autorización manual. Cuando pase
a "Registrado": delegar a Cloudflare → Railway → Custom Domain (CNAME, "DNS only").

---

## 13. Cómo correr localmente

```bash
cd FuturaTecno

# 1) Base de datos (Docker)
docker-compose up -d postgres        # esperar a que esté "healthy"

# 2) Backend (necesita ANTHROPIC_API_KEY en backend/.env)
cd backend && mvn spring-boot:run     # http://localhost:8080

# 3) Frontend (en otra terminal)
cd frontend && npm install && npm run dev   # proxya /api → :8080
```

Probar la imagen Docker de producción localmente: ver `DEPLOY.md`.

---

## 14. Branding

- **Logo** verde lima ("Futura" blanco + "Tecno" lima), bajada "TU TECNOLOGÍA. TU FUTURO.".
- **Paleta:** lima `#C8E048` (acentos/botones), lima profundo `#5D6B14` (texto/links), oscuro `#16181D` (header público + sidebar admin). Contenido sobre fondo claro.
- **Favicon:** ícono "F" sobre cuadrado oscuro redondeado.

---

*Documento generado a partir del código real del repositorio. Ante dudas, la fuente de verdad
es el código en `backend/` y `frontend/`.*
