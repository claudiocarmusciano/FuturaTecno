# Estructura del Proyecto FuturaTecno

```
FuturaTecno/
│
├── README.md                          # Documentación completa
├── QUICKSTART.md                      # Guía rápida de inicio
├── ESTRUCTURA.md                      # Este archivo
├── .env                               # Variables de entorno (local)
├── .env.example                       # Plantilla de .env
├── .gitignore                         # Archivos ignorados por git
│
├── docker-compose.yml                 # Orquestación de servicios
│
├── backend/                           # Aplicación Spring Boot
│   ├── pom.xml                        # Dependencias Maven
│   ├── Dockerfile                     # Imagen Docker del backend
│   │
│   ├── .mvn/
│   │   └── wrapper/
│   │       └── maven-wrapper.properties
│   │
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/futuratecno/
│           │       ├── FuturaTecnoApplication.java          # Main
│           │       │
│           │       ├── domain/                              # Entidades JPA
│           │       │   ├── BaseEntity.java                  # Clase base
│           │       │   ├── Proveedor.java
│           │       │   ├── Producto.java
│           │       │   ├── Variante.java
│           │       │   └── Imagen.java
│           │       │
│           │       ├── infrastructure/                      # Repos + Config
│           │       │   ├── ProveedorRepository.java
│           │       │   ├── ProductoRepository.java
│           │       │   ├── VarianteRepository.java
│           │       │   ├── ImagenRepository.java
│           │       │   └── CorsConfig.java                  # CORS
│           │       │
│           │       ├── application/                         # (Para futuro)
│           │       │   └── [Servicios de negocio]
│           │       │
│           │       └── api/                                 # Controladores REST
│           │           ├── ProveedorController.java
│           │           ├── HealthController.java
│           │           └── dto/
│           │               └── ProveedorDTO.java
│           │
│           └── resources/
│               ├── application.yml                          # Config Spring
│               ├── db/
│               │   └── migration/
│               │       └── V1__Initial_Schema.sql          # Migraciones Flyway
│               └── static/                                  # Archivos estáticos
│
├── frontend/                          # Aplicación React + Vite
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile                     # Imagen Docker + Nginx
│   ├── nginx.conf                     # Configuración Nginx
│   ├── index.html
│   │
│   ├── public/                        # Assets estáticos
│   │   └── favicon.ico (por crear)
│   │
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── App.css
│       ├── index.css
│       │
│       ├── components/
│       │   └── layouts/
│       │       ├── AdminLayout.jsx
│       │       ├── AdminLayout.css
│       │       ├── PublicLayout.jsx
│       │       └── PublicLayout.css
│       │
│       ├── pages/
│       │   ├── admin/
│       │   │   ├── Dashboard.jsx
│       │   │   ├── ProveedoresPage.jsx
│       │   │   ├── ParsingPage.jsx           # (Corazón del sistema)
│       │   │   └── ImagesPage.jsx
│       │   │
│       │   └── public/
│       │       └── CatalogPage.jsx
│       │
│       ├── services/                  # (Para futuro)
│       │   └── api.js                 # Llamadas axios
│       │
│       └── hooks/                     # (Para futuro)
│           └── useApi.js
```

## Flujos Principales

### 1. Parsing con IA (Corazón del Sistema)

```
Administrador
    ↓
ParsingPage (Frontend)
    ↓ POST /api/admin/parsing/preview
Backend (ParsingController) [Por crear]
    ↓
Anthropic API (Claude Haiku)
    ↓ Respuesta parseada
Preview en tabla (Frontend)
    ↓ [Usuario revisa y confirma]
    ↓ POST /api/admin/parsing/confirmar
Base de datos (PostgreSQL)
    ↓
Catálogo actualizado
```

### 2. Carga de Imágenes

```
Frontend (ImagesPage)
    ↓ Drag & Drop
    ↓ POST /api/admin/imagenes (multipart)
Backend
    ↓
Volumen Docker (/app/static/images)
    ↓
Nginx sirve archivos estáticos
    ↓
CatalogPage muestra imágenes
```

### 3. Catálogo Público

```
Usuario
    ↓
CatalogPage (Frontend)
    ↓ GET /api/productos
Backend
    ↓
Base de datos
    ↓
JSON con productos + ETA
    ↓
Grid de productos
    ↓
Detalle con ETA dinámico
```

## Configuración de Puertos

| Servicio | Puerto | URL |
|----------|--------|-----|
| PostgreSQL | 5432 | localhost:5432 |
| Backend Spring Boot | 8080 | http://localhost:8080/api |
| Frontend Nginx | 80 | http://localhost |
| Frontend Dev (Vite) | 5173 | http://localhost:5173 |

## Variables de Entorno

**Backend (application.yml):**
```yaml
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/futuratecno_db
SPRING_DATASOURCE_USERNAME=futuratecno
SPRING_DATASOURCE_PASSWORD=futuratecno
ANTHROPIC_API_KEY=sk-ant-...
CUTOFF_HOUR=14
```

## Notas Importantes

1. **Sin autenticación aún**: El panel admin es accesible sin login. Se agregará Spring Security + JWT en iteración posterior.

2. **Soft Delete**: Los proveedores y productos no se eliminan, solo se marcan como `activo = false`.

3. **Migraciones Flyway**: Se ejecutan automáticamente al iniciar el backend. Archivo: `V1__Initial_Schema.sql`

4. **CORS habilitado**: El backend acepta peticiones desde cualquier origen (configurable).

5. **Imagen storage**: Temporal en disco. Para producción, considerar S3 o similar.

## Proximos Archivos a Crear

- [ ] `ParsingController.java` - Endpoint para parsing con IA
- [ ] `ParsingService.java` - Lógica de parsing
- [ ] `AnthropicClient.java` - Cliente para Anthropic API
- [ ] `ImageController.java` - Endpoint para carga de imágenes
- [ ] `ProductoController.java` - Endpoint para listar productos (catálogo)
- [ ] `ETAService.java` - Cálculo de fechas de entrega
- [ ] `DolarApiClient.java` - Integración con dolarapi.com
- [ ] Tests unitarios y de integración
