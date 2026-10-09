import { useEffect } from 'react'

/**
 * Aparición al hacer scroll (estilo Apple): los elementos con la clase `revelar` dentro de `ref`
 * entran suave cuando llegan a la pantalla. Se vuelve a correr cuando cambian `deps`, porque parte
 * del contenido llega después (destacados, categorías).
 *
 * El contenido solo se oculta cuando este hook marcó el contenedor con `con-animacion`: si el JS
 * falla o el navegador no tiene IntersectionObserver, todo queda visible. Con "reducir movimiento"
 * activado en el sistema, el CSS no anima nada.
 */
export default function useRevelar(ref, deps = []) {
  useEffect(() => {
    const raiz = ref.current
    if (!raiz) return undefined
    const pendientes = [...raiz.querySelectorAll('.revelar:not(.visible)')]
    // Pestaña en segundo plano o navegador sin IntersectionObserver: se muestra todo de una, sin
    // animación. En una pestaña oculta el observador no avisa nada hasta que se la mira, y no
    // queremos depender de eso para que el contenido exista.
    if (!('IntersectionObserver' in window) || document.visibilityState !== 'visible') {
      pendientes.forEach(el => el.classList.add('visible'))
      return undefined
    }
    raiz.classList.add('con-animacion')
    // Red de seguridad: si por cualquier motivo el observador no avisa, a los 2,5 s se muestra
    // todo lo que ya está arriba del borde inferior de la pantalla.
    const respaldo = window.setTimeout(() => {
      pendientes.forEach(el => { if (el.getBoundingClientRect().top < window.innerHeight) el.classList.add('visible') })
    }, 2500)
    const obs = new IntersectionObserver((entradas) => {
      for (const e of entradas) {
        if (e.isIntersecting) {
          e.target.classList.add('visible')
          obs.unobserve(e.target)
        }
      }
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.12 })
    pendientes.forEach(el => obs.observe(el))
    return () => { obs.disconnect(); window.clearTimeout(respaldo) }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
