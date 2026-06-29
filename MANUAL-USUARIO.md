# 🧑‍💻 Manual de Usuario — FuturaTecno

Guía simple y paso a paso para manejar tu tienda **FuturaTecno**. No hace falta saber de
programación: con esto cargás productos, ponés imágenes y administrás todo desde el panel.

- **Tu tienda (lo que ven los clientes):** https://futuratecno-production.up.railway.app
- **Tu panel de administración:** https://futuratecno-production.up.railway.app/admin

---

## 1. Entrar al panel

1. Entrá a **https://futuratecno-production.up.railway.app/admin**
2. Poné tu **email** y **contraseña** de administrador.
3. Listo: vas a ver el **Dashboard** con el resumen de tu tienda.

> 💡 Si te olvidás la contraseña, se cambia desde la configuración de la app (pedímelo y te ayudo).

En el menú de la izquierda (en el celular, tocá el botón **☰** arriba a la derecha) tenés:
**Dashboard · Proveedores · Parsing IA · Productos · Imágenes · Usuarios**.

---

## 2. El recorrido para publicar productos (resumen)

> Este es el orden recomendado. Cada paso se explica en detalle más abajo.

```
1) Cargás un PROVEEDOR (con su % de margen y % de flete)
        ↓
2) Pegás la lista de WhatsApp en PARSING IA → se arman los productos solos
        ↓
3) Revisás y confirmás
        ↓
4) Ponés las IMÁGENES
        ↓
   ✅ ¡Ya están publicados en tu catálogo!
```

---

## 3. Cargar un proveedor

Un "proveedor" es quien te pasa las listas de precios. Cada uno tiene su **margen** y su **flete**.

1. Menú → **Proveedores** → **Nuevo Proveedor**.
2. Completá:
   - **Nombre** del proveedor.
   - **% Margen**: tu ganancia (ej: 15).
   - **% Flete**: el costo de envío como porcentaje (ej: 5).
3. **Guardar**.

> 💡 El margen y el flete se aplican a **todos** los productos de ese proveedor. El sistema calcula
> el precio de venta solo (ver sección 7).

Para **editar** o **eliminar** un proveedor, usá los botones de su fila.

---

## 4. Cargar productos desde WhatsApp (Parsing IA)

Esta es la magia: pegás el texto que te mandó el proveedor y la **inteligencia artificial** arma
los productos automáticamente.

1. Menú → **Parsing IA**.
2. Elegí el **proveedor** correspondiente.
3. **Copiá y pegá** el texto de la lista de WhatsApp (tal cual, con desprolijidades y todo).
4. Apretá **Procesar / Analizar**.
5. La IA te muestra un **preview**: marca, modelo, características, precio y moneda de cada producto.
6. **Revisá** que esté todo bien (podés corregir lo que haga falta).
7. **Confirmá** → los productos quedan cargados.

> 💡 Si volvés a pasar una lista del mismo proveedor, el sistema **actualiza los precios** de los
> productos que ya existían, en vez de duplicarlos.

---

## 5. Revisar y editar productos

Menú → **Productos**: ahí ves todo tu catálogo.

- **Editar** (✏️): cambiás categoría, marca, modelo, características, **costo**, moneda y stock.
  El precio de venta se recalcula solo.
- **Eliminar** (🗑️): saca el producto del catálogo.

> 💡 Vos cargás el **costo**; la tienda muestra el **precio de venta** (costo + flete + margen).

---

## 6. Poner imágenes a los productos

Menú → **Imágenes**. Tenés **dos formas**:

### A) Automática (rápida, para muchos de una)
1. Apretá **"Buscar imágenes faltantes (automático)"**.
2. El sistema busca solo en sitios oficiales de marca y tiendas argentinas, y completa las que encuentra.

### B) Manual (la más confiable, recomendada) 👈
Ideal cuando la automática no encontró, o querés elegir la foto exacta. Tu método:

1. En la fila del producto, tocá **🔍 Buscar** → se abre **Google Imágenes** con ese producto.
   *(También podés buscarlo directamente en MercadoLibre.)*
2. Encontrá la foto que te gusta, **click derecho** sobre ella → **"Copiar dirección de imagen"**.
3. Volvé a FuturaTecno y **pegá** esa dirección en el campo **URL** de la fila.
4. Vas a ver una **miniatura de preview** al instante. Si está bien, apretá **Guardar**.

> 💡 Tip: elegí imágenes con **fondo blanco** y que se vea bien el producto. Quedan mucho más prolijas en el catálogo.

---

## 7. ¿Cómo se calcula el precio de venta?

Vos cargás el **costo** del producto. La tienda le suma el flete y tu margen:

> **Precio de venta = Costo × (1 + Flete%) × (1 + Margen%)**

**Ejemplo:** costo US$ 410, flete 5%, margen 15%
→ 410 × 1,05 × 1,15 = **US$ 495,08**

Y el precio en pesos se calcula con la **cotización del dólar oficial del día** (se actualiza sola).
Por eso, si sube el dólar, los precios en pesos se ajustan automáticamente.

---

## 8. Ver tus clientes registrados

Menú → **Usuarios**: ahí está la **base de mails** de las personas que se registraron en tu tienda.
Podés **exportarla a CSV** (para usar en email marketing, por ejemplo).

> 💡 Los clientes que se registran **no** pueden entrar al panel; solo vos (administrador).

---

## 9. Cómo se ve la tienda para tus clientes

En **https://futuratecno-production.up.railway.app** tus clientes ven:

- El **catálogo** con buscador, filtros (categoría, marca, precio) y orden por precio.
- Cada producto con su **precio en USD y en pesos**, y la **fecha estimada de entrega**.
- Al entrar a un producto, un **botón de WhatsApp** que abre un chat con vos, con el producto ya
  identificado en el mensaje.

> ℹ️ Por privacidad, el catálogo **no** muestra el proveedor ni el número exacto de stock
> (dice "Stock sujeto a disponibilidad").

---

## 10. Preguntas frecuentes

**¿Por qué no aparece en la tienda lo que cargué probando en mi computadora?**
La tienda en internet usa una base **separada** de la de tu computadora. Lo que ves publicado es
lo que cargás directamente en el panel online (`/admin`).

**Subí un producto pero no tiene imagen, ¿se publica igual?**
Sí, se publica; muestra un "Sin imagen". Podés agregarle la foto cuando quieras desde **Imágenes**.

**Cambié el margen de un proveedor, ¿se actualizan los precios?**
Sí, automáticamente, en todos los productos de ese proveedor.

**¿Cada cuánto se actualiza el dólar?**
Solo, varias veces al día (dólar oficial).

**¿Puedo usar mi propio dominio (futuratecno.com.ar)?**
Sí, en cuanto NIC.ar apruebe el registro del dominio se conecta. Mientras tanto la tienda funciona
perfecto con la dirección de arriba.

---

*¿Algo no se entiende o querés sumar una función? Anotalo y lo vemos.* 🚀
