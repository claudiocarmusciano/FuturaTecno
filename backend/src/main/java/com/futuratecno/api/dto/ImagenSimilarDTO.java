package com.futuratecno.api.dto;

/**
 * Una imagen que ya está en el catálogo y podría servir para otro producto.
 *
 * <p>Se devuelve con el producto de origen a la vista —marca, modelo y si está publicado— porque
 * el admin necesita saber DE QUÉ es la foto para decidir. Una miniatura suelta no alcanza: el
 * riesgo de esta pantalla es pegarle a un producto la foto de otro parecido, y esa elección
 * después queda recordada por marca+modelo y se reusa en cada carga futura.
 *
 * @param exacto   true si el origen es el mismo artículo redactado distinto (misma clave suelta),
 *                 false si es apenas de la misma familia y hay que mirarla con más cuidado.
 * @param afinidad cuántos caracteres del modelo coinciden desde el principio, ya descontada la
 *                 marca. Sirve para que la pantalla muestre lo floja que es una coincidencia en
 *                 vez de presentarlas todas igual: "LDC16255" y "LDC15255" comparten 4 y son
 *                 notebooks distintas.
 */
public record ImagenSimilarDTO(Long productoId,
                               String marca,
                               String modelo,
                               String url,
                               boolean activo,
                               boolean exacto,
                               int afinidad) {
}
