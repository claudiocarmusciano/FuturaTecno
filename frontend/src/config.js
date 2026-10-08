// Configuración del frontend. Cambiá estos valores cuando lo necesites.

// Número de WhatsApp del negocio (con código de país, sin "+" ni espacios).
// Ej: 5492284622222  → +54 9 2284 62-2222
export const WHATSAPP_NUMBER = '5492284324444'

// Nombre del negocio (se usa en mensajes y textos).
export const NOMBRE_NEGOCIO = 'Tecnópolis Olavarría'

// Marca anterior: se menciona en el pie legal mientras dure la mudanza ("antes Futura Tecno").
export const NOMBRE_ANTERIOR = 'Futura Tecno'

// Datos legales del titular (pie de página y páginas legales). Tienen que coincidir con la
// constancia de ARCA: "Tecnópolis Olavarría" es el nombre comercial de una persona física.
export const TITULAR = {
  nombre: 'Claudio José Carmusciano',
  cuit: '20-23128286-7',
  condicionIva: 'Responsable Inscripto',
  domicilioFiscal: 'Pringles 2169, Olavarría (7400), Buenos Aires',
  domicilioComercial: 'San Martín 2821, Olavarría (7400), Buenos Aires'
}

// Horario de atención del local (se muestra en la home y en el pie).
export const HORARIO_LOCAL = '9:30 a 12:30 y 16:30 a 19:30'

// Beneficio de Armá tu PC: se anuncia en la home y en /arma-tu-pc.
export const PROMO_ARMADO = {
  titulo: 'Armado e instalación de regalo',
  texto: 'Si comprás todos los componentes de tu PC en Tecnópolis, te la armamos y te instalamos Windows + Office sin cargo.'
}

export const CONTACTO_EMAIL = 'tecnopolisolavarria@gmail.com'
export const INSTAGRAM_URL = 'https://www.instagram.com/tecnopolisolavarria/'

// Formulario de reclamos de Defensa del Consumidor (Nación).
export const DEFENSA_CONSUMIDOR_URL = 'https://www.argentina.gob.ar/produccion/defensadelconsumidor/formulario'

// Data Fiscal de ARCA: pegar acá el link que da ARCA al generar el QR (y el de la imagen).
// Vacíos = el pie no muestra el QR.
export const DATA_FISCAL_URL = ''
export const DATA_FISCAL_IMG = ''

// Título general de la pestaña: el mismo que el <title> de index.html.
export const TITULO_SITIO = 'Tecnópolis Olavarría — Tecnología en Olavarría | Celulares, notebooks y PC'
