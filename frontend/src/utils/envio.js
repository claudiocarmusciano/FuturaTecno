// Andreani devuelve el código de la modalidad en su propia jerga ("estándar", "sucursal").
// También existe una modalidad local propia. El código se traduce para el cliente, pero se manda
// al backend tal como está para que este recotice o preserve el envío gratuito según corresponda.
export const ETIQUETA_ENVIO = {
  'entrega-local-olavarria': 'Envío gratis dentro de Olavarría',
  'retiro-local': 'Retiro en el local (San Martín 2821, Olavarría)',
  'estándar': 'Envío a tu domicilio',
  'sucursal': 'Retiro en sucursal Andreani',
  'llega hoy': 'Llega hoy (a domicilio)',
  'bigger': 'Envío de paquete grande'
}
export const etiquetaEnvio = (codigo) => ETIQUETA_ENVIO[codigo] || `Envío ${codigo}`
