# FuturaTecno

Plataforma de venta online de tecnología (celulares, accesorios) para Olavarría, Buenos Aires.

## Stack Tecnológico

- **Backend**: Java 21 + Spring Boot 3.3
- **Base de datos**: PostgreSQL 16
- **Frontend**: React 18 + Vite
- **Contenedores**: Docker + Docker Compose
- **IA**: Anthropic Claude API (Haiku 4.5)

## Estructura del Proyecto

```
FuturaTecno/
├── backend/
│   ├── src/main/
│   │   ├── java/com/futuratecno/
│   │   │   ├── domain/          # Entidades JPA
│   │   │   ├── application/     # Servicios de negocio
│   │   │   ├── infrastructure/  # Repositorios
│   │   │   └── api/             # Controladores REST
│   │   └── resources/
│   │       ├── db/migration/    # Scripts Flyway
│   │       └── application.yml  # Configuración
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/          # Componentes React
│   │   ├── pages/               # Páginas
│   │   ├── services/            # Servicios API
│   │   └── App.jsx
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.js
│   └── nginx.conf
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## Prerrequisitos

- Docker 20.10+
- Docker Compose 2.0+
- Node.js 20+ (para desarrollo local frontend)
- Java 21 (para desarrollo local backend)
- Maven 3.8+ (para desarrollo local backend)

## Instalación y Ejecución

### 1. Clonar el repositorio

```bash
git clone <repo-url>
cd FuturaTecno
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Edita .env y configura tus valores, especialmente ANTHROPIC_API_KEY
```

### 3. Levantar los servicios con Docker Compose

```bash
docker-compose up --build
```

Esto levantará:
- **PostgreSQL** en puerto 5432
- **Backend Spring Boot** en puerto 8080 (con contexto `/api`)
- **Frontend Nginx** en puerto 80

### 4. Acceder a la aplicación

- **Frontend público**: http://localhost
- **Panel admin**: http://localhost/admin
- **Health check**: http://localhost:8080/api/health
- **API Proveedores**: http://localhost:8080/api/admin/proveedores

### 5. Detener los servicios

```bash
docker-compose down
```

## Desarrollo Local

### Backend

```bash
cd backend
mvn spring-boot:run
```

Accesible en http://localhost:8080/api

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Accesible en http://localhost:5173

## Migraciones de Base de Datos

Las migraciones se ejecutan automáticamente al iniciar el backend usando Flyway.

Ubicación: `backend/src/main/resources/db/migration/`

## API Endpoints

### Proveedores
- `GET /api/admin/proveedores` - Listar todos
- `GET /api/admin/proveedores/{id}` - Obtener por ID
- `POST /api/admin/proveedores` - Crear
- `PUT /api/admin/proveedores/{id}` - Actualizar
- `DELETE /api/admin/proveedores/{id}` - Eliminar (soft delete)

### Parsing (Por implementar)
- `POST /api/admin/parsing/preview` - Previsualizar parsing
- `POST /api/admin/parsing/confirmar` - Confirmar e importar

## Modelo de Datos

### Proveedor
```java
- id: Long (PK)
- nombre: String (UNIQUE)
- margenPorcentaje: BigDecimal
- costoFleteUsd: BigDecimal
- activo: Boolean
```

### Producto
```java
- id: Long (PK)
- proveedorId: Long (FK)
- marca: String
- modelo: String
- activo: Boolean
```

### Variante
```java
- id: Long (PK)
- productoId: Long (FK)
- almacenamientoGb: Integer
- ramGb: Integer
- color: String
- costoUsd: BigDecimal
- stock: Integer
- activo: Boolean
```

### Imagen
```java
- id: Long (PK)
- varianteId: Long (FK)
- url: String
- orden: Integer
- activo: Boolean
```

## Fórmula de Precio de Venta

```
Precio Venta = (Costo Proveedor USD + Costo Proporcional Expreso USD) × (1 + % Margen)
```

## ETA Dinámica (Por implementar)

- Cut-off time configurable: `CUTOFF_HOUR=14` (14:00 ARG)
- Descuenta fines de semana y feriados nacionales argentinos
- Rango: 2 a 3 días hábiles

## Variables de Entorno

| Variable | Descripción | Defecto |
|----------|-------------|---------|
| `ANTHROPIC_API_KEY` | Clave de API de Anthropic | (requerida) |
| `CUTOFF_HOUR` | Hora de corte para pedidos | 14 |
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | jdbc:postgresql://postgres:5432/futuratecno_db |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | futuratecno |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña BD | futuratecno |

## Roadmap

### Fase 1 (Actual)
- ✅ Scaffolding base del proyecto
- ✅ Modelo de datos
- ✅ CRUD de Proveedores
- ⏳ Parsing con IA (Anthropic)
- ⏳ Carga de imágenes

### Fase 2
- Catálogo público
- Cálculo de ETA dinámica
- Integración con dolarapi.com

### Fase 3
- Autenticación (Spring Security + JWT)
- Sistema de pedidos
- Notificaciones por email

## Troubleshooting

### Error de conexión a PostgreSQL
```
Espera a que la BD esté lista. Comprueba con:
docker-compose logs postgres
```

### Purge de base de datos
```bash
docker-compose down -v
docker-compose up --build
```

### Problemas con puertos ocupados
```bash
# Liberar puerto (ejemplo: 5432)
lsof -ti:5432 | xargs kill -9
```

## Contribución

Por favor, respeta la estructura de carpetas y convenciones de nombrado.

## Licencia

[Por definir]
