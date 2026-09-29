// Demora de entrega de un producto cuando no es la normal de la tienda (viene del proveedor,
// que el catálogo nunca nombra). Null = entrega normal.
export const textoDemora = p =>
  p?.demoraEntregaMinDias ? `${p.demoraEntregaMinDias} a ${p.demoraEntregaMaxDias} días` : null
