import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { iniciarAnalitica, paginaVista, contactoWhatsapp } from '../utils/analitica'

/**
 * Página vista en cada cambio de ruta (la tienda es una SPA: sin esto GA4 y el Pixel verían una
 * sola página por visita) y clic a WhatsApp desde cualquier link wa.me del sitio.
 */
export default function Analitica() {
  const location = useLocation()

  useEffect(() => {
    iniciarAnalitica()
    const alHacerClic = (e) => {
      const a = e.target.closest?.('a[href*="wa.me/"]')
      if (a) contactoWhatsapp(window.location.pathname)
    }
    document.addEventListener('click', alHacerClic, true)
    return () => document.removeEventListener('click', alHacerClic, true)
  }, [])

  useEffect(() => {
    paginaVista(location.pathname + location.search)
  }, [location.pathname, location.search])

  return null
}
