# 🚀 Quick Start - FuturaTecno

## Inicio Rápido con Docker Compose

### 1. Preparar el ambiente

```bash
cd /Users/claudiocarmusciano/Desktop/Proyectos/FuturaTecno

# Copiar archivo de ejemplo (si es necesario)
# cp .env.example .env

# ⚠️ IMPORTANTE: Configurar tu ANTHROPIC_API_KEY en el archivo .env
# antes de continuar
```

### 2. Levantar los servicios

```bash
docker-compose up --build
```

**Esperar a que todos los servicios estén listos:**
- PostgreSQL: `database system is ready to accept connections`
- Backend: `Tomcat started on port(s): 8080`
- Frontend: Se inicia automáticamente después de backend

### 3. Acceder a la aplicación

| Servicio | URL |
|----------|-----|
| Frontend (Catálogo) | http://localhost |
| Panel Admin | http://localhost/admin |
| API Health Check | http://localhost:8080/api/health |
| API Proveedores | http://localhost:8080/api/admin/proveedores |

### 4. Detener los servicios

```bash
docker-compose down
```

## Desarrollo Local (sin Docker)

### Backend

```bash
cd backend
mvn spring-boot:run
# Accesible en http://localhost:8080/api
```

Asegúrate de tener PostgreSQL corriendo localmente en puerto 5432.

### Frontend

```bash
cd frontend
npm install
npm run dev
# Accesible en http://localhost:5173
```

## Probar la API

### Crear un proveedor

```bash
curl -X POST http://localhost:8080/api/admin/proveedores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Proveedor Test",
    "margenPorcentaje": 30.00,
    "costoFleteUsd": 10.00
  }'
```

### Listar proveedores

```bash
curl http://localhost:8080/api/admin/proveedores
```

## Próximos Pasos

1. **Implementar Parsing con IA**
   - `POST /api/admin/parsing/preview`
   - `POST /api/admin/parsing/confirmar`

2. **Carga de Imágenes**
   - Implementar drag & drop en frontend
   - Configurar almacenamiento en volumen Docker

3. **Catálogo Público**
   - Implementar endpoints para listar productos
   - Agregar filtros y búsqueda

4. **ETA Dinámica**
   - Implementar cálculo de fechas de entrega
   - Integrar con calendario de feriados argentinos

## Troubleshooting

### ❌ Error: "Couldn't create temporary directory"

Asegúrate de que Docker tiene acceso a recursos suficientes.

```bash
docker system prune  # Limpiar recursos no usados
docker-compose up --build  # Reintentar
```

### ❌ Error: "Database is locked"

Espera unos segundos a que PostgreSQL termine su inicialización.

```bash
docker-compose logs postgres  # Ver logs
```

### ❌ Frontend muestra "Cannot GET /"

Espera a que el build del frontend termine. Comprueba:

```bash
docker-compose logs frontend
```

### ✅ ¿Todo correcto?

Verifica que puedas acceder a:
```bash
curl http://localhost:8080/api/health
# Esperado: {"status":"UP"}
```

---

Para documentación completa, ver [README.md](README.md)
