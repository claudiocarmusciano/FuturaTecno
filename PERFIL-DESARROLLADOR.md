# Brief para LLM — Perfil de Desarrollador y Promoción de Servicios

> **Propósito de este documento:** servir de insumo a un modelo de lenguaje (LLM) para que arme una página web que informe y promocione el tipo de aplicaciones que desarrollo. Contiene quién soy, qué construyo, con qué tecnologías, qué técnicas me diferencian, ejemplos reales y lineamientos para la página.

---

## 1. Quién soy

Desarrollador de **aplicaciones web a medida** orientadas a **automatizar procesos reales de negocios** (pymes, comercios, emprendimientos, clubes). Trabajo desde **Olavarría, Argentina**, y construyo soluciones pensadas para el mercado argentino y latinoamericano: precios en pesos y dólares, integración con WhatsApp, cotizaciones en tiempo real, etc.

Mi enfoque: **resolver un dolor concreto del cliente con software que se usa todos los días**, no demos vistosas sin uso real. Priorizo que la herramienta ahorre tiempo, reduzca tareas manuales y se integre al flujo de trabajo que el cliente ya tiene.

---

## 2. Qué tipo de aplicaciones hago

- **Plataformas de e-commerce / catálogos online** con panel de administración.
- **Automatización de carga de datos con Inteligencia Artificial** (ej: convertir listas de precios desordenadas de WhatsApp en productos estructurados).
- **Sistemas de gestión multi-tenant (SaaS)** donde varios clientes usan la misma plataforma de forma aislada.
- **Aplicaciones de gestión deportiva** (fixtures de torneos, administración de clubes).
- **Integraciones con servicios externos**: APIs de cotización de monedas, búsqueda web, WhatsApp, fuentes de datos de productos.

Patrón común: **back-office potente + cara pública simple**, con IA donde aporta valor real (parseo, búsqueda, clasificación).

---

## 3. Stack tecnológico

**Backend**
- **Java 21 + Spring Boot 3.3** (API REST, inyección de dependencias, capas domain/application/infrastructure)
- **PostgreSQL 16** como base de datos relacional
- **Flyway** para migraciones versionadas del esquema
- **JPA / Hibernate** para el mapeo objeto-relacional
- **Docker / Docker Compose** para entornos reproducibles

**Frontend**
- **React 18 + Vite** (SPA rápida y moderna)
- **React Router** para navegación y URLs compartibles
- **Axios** para consumo de APIs
- Componentes con filtros dinámicos, estados de carga y diseño responsive

**Inteligencia Artificial**
- **API de Anthropic (Claude)** — modelo **Haiku 4.5** por su excelente relación costo/rendimiento
- Uso de **herramientas de Claude**: parseo de texto no estructurado, **búsqueda web** integrada
- Diseño de prompts robustos y manejo de respuestas con tolerancia a errores

**Prácticas de desarrollo**
- Control de versiones con **Git/GitHub**, manejo cuidadoso de secretos (variables de entorno, `.gitignore`, nunca credenciales en el repo)
- Configuración por entorno (`.env`, perfiles)
- Lógica de negocio testeable y separada de la infraestructura

---

## 4. Técnicas y diferenciales (lo que me distingue)

1. **Parseo inteligente con IA.** Tomo datos caóticos del mundo real —una lista de precios pegada de WhatsApp, con emojis, abreviaturas y formatos inconsistentes— y con Claude la convierto en datos estructurados (marca, modelo, especificaciones, precio, categoría) listos para la base de datos.

2. **Conversión de monedas en tiempo real.** Integro cotizaciones (ej: dólar oficial de `dolarapi.com`) para mostrar precios en USD y ARS automáticamente, con caché y valores de respaldo para que nunca falle.

3. **Obtención automática de imágenes de producto.** Combino la **búsqueda web de Claude** con extracción de metadatos (`og:image`) de la página del producto para conseguir fotos reales (oficiales de la marca o de MercadoLibre) sin intervención manual y a costo muy bajo (~US$0,01 por producto).

4. **Catálogos con filtros dinámicos.** Las categorías y marcas de filtro se generan solas según los productos cargados en el momento — no hay categorías fijas hardcodeadas.

5. **Integración con WhatsApp como canal de venta.** Botones de "Consultar por WhatsApp" con mensaje pre-armado que identifica el producto, convirtiendo el catálogo en una herramienta de venta directa.

6. **Pensado para Argentina.** Precios duales, cotización del día, lenguaje local, y soluciones que contemplan las restricciones reales (bloqueos de ISP, costos en dólares, etc.) eligiendo siempre la alternativa más confiable y económica.

---

## 5. Ejemplos de proyectos reales

### FuturaTecno — Plataforma de venta de tecnología
Catálogo online para venta de tecnología (notebooks, celulares, accesorios Apple, etc.) con:
- **Carga de productos por IA**: el admin pega la lista de precios de WhatsApp del proveedor y Claude la parsea automáticamente, detectando categorías a partir de los títulos de sección.
- **Precios USD + ARS** calculados con margen, flete y cotización del dólar oficial en vivo.
- **Imágenes automáticas** de cada producto vía búsqueda web de Claude + `og:image`.
- **Catálogo público** con buscador, filtros dinámicos por categoría/marca, página de detalle por producto y botón de consulta por WhatsApp.
- **Panel de administración**: gestión de proveedores, parsing con previsualización editable, gestión de imágenes (automática + manual), e importación con actualización de precios.

### Aplicaciones de gestión deportiva (padel)
Sistemas para clubes y torneos de pádel: generación de **fixtures**, reglas de sorteo, administración de torneos y plataforma **multi-tenant** (varios clubes en una misma instancia, con datos aislados).

> *(El LLM puede pedir más detalles o usar estos ejemplos como casos de estudio en la página.)*

---

## 6. Para quién (clientes ideales)

- **Comercios y pymes** que cargan o actualizan muchos productos por día y pierden horas en tareas manuales.
- **Emprendedores** que venden por WhatsApp/Instagram y necesitan un catálogo profesional online.
- **Clubes y organizadores** de eventos/torneos que necesitan digitalizar su gestión.
- En general: cualquier negocio con un **proceso repetitivo y manual** que se pueda automatizar con software + IA.

---

## 7. Propuesta de valor (mensajes clave para la página)

- **"Convierto tareas manuales en procesos automáticos."**
- **"Software a medida que se usa todos los días, no demos vacías."**
- **"Inteligencia Artificial aplicada a problemas reales de tu negocio."**
- **"Pensado para Argentina: precios en pesos y dólares, integración con WhatsApp, cotización del día."**
- **"De la lista de WhatsApp al catálogo online, automáticamente."**

---

## 8. Lineamientos para armar la página (instrucciones para el LLM)

**Tono:** profesional pero cercano, claro, sin jerga técnica excesiva (el cliente final no es programador). Español rioplatense/neutro.

**Secciones sugeridas:**
1. **Hero** — titular potente + subtítulo + CTA ("Contactame por WhatsApp"). Idea de titular: *"Automatizo tu negocio con aplicaciones a medida e Inteligencia Artificial."*
2. **Qué hago** — 3-4 tarjetas con los tipos de aplicaciones (e-commerce con IA, automatización, gestión, integraciones).
3. **Cómo trabajo / diferenciales** — los puntos de la sección 4, explicados en lenguaje de beneficio (no de tecnología).
4. **Proyectos / casos** — FuturaTecno y las apps de pádel como prueba social.
5. **Tecnologías** — un bloque discreto con los logos/nombres del stack (para dar confianza técnica).
6. **CTA final** — formulario simple o botón de WhatsApp.

**Llamado a la acción principal:** contacto por **WhatsApp** (es el canal natural del público objetivo).

**Qué evitar:** prometer cosas que no se pueden cumplir, lenguaje 100% técnico, diseño recargado. Mantener foco en el **beneficio para el cliente** (ahorro de tiempo, más ventas, profesionalismo).

**Datos a completar por el usuario antes de publicar:** nombre comercial / marca personal, número de WhatsApp de contacto, email, enlaces a redes o portfolio, y precios o modalidad de trabajo si se desea mostrar.
