# FuturaTecno - API Reference

Base URL: `http://localhost:8080/api`

## Health Check

### Verificar que el backend está vivo

```http
GET /health

Response: 200 OK
{
  "status": "UP"
}
```

## Proveedores

### Listar todos los proveedores

```http
GET /admin/proveedores

Response: 200 OK
[
  {
    "id": 1,
    "nombre": "Proveedor CABA",
    "margenPorcentaje": 30.50,
    "costoFleteUsd": 15.00,
    "activo": true,
    "createdAt": "2026-06-01T10:00:00",
    "updatedAt": "2026-06-01T10:00:00"
  }
]
```

### Obtener un proveedor

```http
GET /admin/proveedores/:id

Response: 200 OK
{
  "id": 1,
  "nombre": "Proveedor CABA",
  "margenPorcentaje": 30.50,
  "costoFleteUsd": 15.00,
  "activo": true,
  "createdAt": "2026-06-01T10:00:00",
  "updatedAt": "2026-06-01T10:00:00"
}
```

### Crear un proveedor

```http
POST /admin/proveedores

Content-Type: application/json

{
  "nombre": "Proveedor Buenos Aires",
  "margenPorcentaje": 25.00,
  "costoFleteUsd": 12.50
}

Response: 201 Created
{
  "id": 2,
  "nombre": "Proveedor Buenos Aires",
  "margenPorcentaje": 25.00,
  "costoFleteUsd": 12.50,
  "activo": true,
  "createdAt": "2026-06-01T11:00:00",
  "updatedAt": "2026-06-01T11:00:00"
}
```

### Actualizar un proveedor

```http
PUT /admin/proveedores/:id

Content-Type: application/json

{
  "nombre": "Proveedor Buenos Aires Actualizado",
  "margenPorcentaje": 28.00,
  "costoFleteUsd": 14.00
}

Response: 200 OK
{
  "id": 2,
  "nombre": "Proveedor Buenos Aires Actualizado",
  "margenPorcentaje": 28.00,
  "costoFleteUsd": 14.00,
  "activo": true,
  "createdAt": "2026-06-01T11:00:00",
  "updatedAt": "2026-06-01T12:00:00"
}
```

### Eliminar un proveedor (soft delete)

```http
DELETE /admin/proveedores/:id

Response: 204 No Content
```

## Parsing (Por Implementar)

### Previsualizar parsing de lista WhatsApp

```http
POST /admin/parsing/preview

Content-Type: application/json

{
  "proveedorId": 1,
  "textoWhatsapp": "iPhone 15 Pro 256GB Titanio $899USD\niPhone 15 256GB Azul $799USD\nSamsung S24 128GB Gris $699USD"
}

Response: 200 OK
[
  {
    "marca": "Apple",
    "modelo": "iPhone 15 Pro",
    "almacenamientoGb": 256,
    "color": "Titanio",
    "costoUsd": 899.00,
    "stock": 0
  },
  {
    "marca": "Apple",
    "modelo": "iPhone 15",
    "almacenamientoGb": 256,
    "color": "Azul",
    "costoUsd": 799.00,
    "stock": 0
  }
]
```

### Confirmar e importar parsing

```http
POST /admin/parsing/confirmar

Content-Type: application/json

{
  "proveedorId": 1,
  "variantes": [
    {
      "marca": "Apple",
      "modelo": "iPhone 15 Pro",
      "almacenamientoGb": 256,
      "ramGb": 8,
      "color": "Titanio",
      "costoUsd": 899.00,
      "stock": 10
    }
  ]
}

Response: 200 OK
{
  "productosCreados": 1,
  "variantesCreadas": 1,
  "mensaje": "Importación exitosa"
}
```

## Imágenes (Por Implementar)

### Subir imagen para una variante

```http
POST /admin/imagenes

Content-Type: multipart/form-data

FormData:
  - varianteId: 1
  - imagen: <binary file>
  - orden: 1

Response: 201 Created
{
  "id": 1,
  "varianteId": 1,
  "url": "/api/static/images/variante_1_1.jpg",
  "orden": 1,
  "activo": true
}
```

### Listar imágenes de una variante

```http
GET /admin/variantes/:varianteId/imagenes

Response: 200 OK
[
  {
    "id": 1,
    "varianteId": 1,
    "url": "/api/static/images/variante_1_1.jpg",
    "orden": 1,
    "activo": true
  },
  {
    "id": 2,
    "varianteId": 1,
    "url": "/api/static/images/variante_1_2.jpg",
    "orden": 2,
    "activo": true
  }
]
```

## Productos / Catálogo (Por Implementar)

### Listar productos del catálogo público

```http
GET /productos

Query params:
  - marca: (opcional) "Apple"
  - almacenamiento: (opcional) 256
  - ram: (opcional) 8
  - precioMin: (opcional) 100
  - precioMax: (opcional) 1500
  - page: (opcional) 0
  - size: (opcional) 20

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "marca": "Apple",
      "modelo": "iPhone 15 Pro",
      "precio": {
        "usd": 899.00,
        "ars": 276945.00,
        "tasaDolar": 308.05
      },
      "variantes": [
        {
          "id": 1,
          "almacenamientoGb": 256,
          "ramGb": 8,
          "color": "Titanio",
          "stock": 5
        }
      ],
      "imagenes": [
        {
          "url": "/static/images/product_1_1.jpg",
          "orden": 1
        }
      ],
      "eta": {
        "desde": "2026-06-04",
        "hasta": "2026-06-05",
        "dias": "2-3 días hábiles"
      }
    }
  ],
  "totalPages": 3,
  "totalElements": 47,
  "currentPage": 0
}
```

### Obtener detalle de un producto

```http
GET /productos/:productoId

Response: 200 OK
{
  "id": 1,
  "marca": "Apple",
  "modelo": "iPhone 15 Pro",
  "proveedor": {
    "id": 1,
    "nombre": "Proveedor CABA"
  },
  "variantes": [
    {
      "id": 1,
      "almacenamientoGb": 256,
      "ramGb": 8,
      "color": "Titanio",
      "costoUsd": 899.00,
      "precioVenta": {
        "usd": 1189.17,
        "ars": 366281.86
      },
      "stock": 5
    }
  ],
  "imagenes": [
    {
      "url": "/static/images/product_1_1.jpg",
      "orden": 1
    },
    {
      "url": "/static/images/product_1_2.jpg",
      "orden": 2
    }
  ],
  "eta": {
    "desde": "2026-06-04T14:00:00",
    "hasta": "2026-06-05T14:00:00",
    "mensaje": "Retirás entre el miércoles 4 y el jueves 5 de junio"
  }
}
```

## Códigos de Respuesta

| Código | Significado |
|--------|-------------|
| 200 | OK - Solicitud exitosa |
| 201 | Created - Recurso creado |
| 204 | No Content - Solicitud exitosa sin contenido |
| 400 | Bad Request - Datos inválidos |
| 404 | Not Found - Recurso no encontrado |
| 500 | Internal Server Error - Error del servidor |

## Errores Comunes

### Error: No se encuentra el proveedor

```json
{
  "error": "Proveedor no encontrado",
  "status": 404
}
```

### Error: Datos inválidos

```json
{
  "error": "El nombre del proveedor es obligatorio",
  "status": 400,
  "field": "nombre"
}
```

## Ejemplos con curl

```bash
# Crear un proveedor
curl -X POST http://localhost:8080/api/admin/proveedores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Proveedor Test",
    "margenPorcentaje": 30,
    "costoFleteUsd": 10
  }'

# Listar proveedores
curl http://localhost:8080/api/admin/proveedores

# Obtener un proveedor
curl http://localhost:8080/api/admin/proveedores/1

# Actualizar un proveedor
curl -X PUT http://localhost:8080/api/admin/proveedores/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Proveedor Actualizado",
    "margenPorcentaje": 35,
    "costoFleteUsd": 12
  }'

# Eliminar un proveedor
curl -X DELETE http://localhost:8080/api/admin/proveedores/1

# Health check
curl http://localhost:8080/api/health
```

## Notas

1. Todos los endpoints esperan/devuelven JSON
2. Los timestamps están en formato ISO 8601 (UTC)
3. Los precios están en USD por defecto
4. El cálculo de ARS es: `USD × tasaDolar (obtenida de dolarapi.com)`
5. Soft delete: los proveedores marcados como `activo=false` no aparecen en listados
