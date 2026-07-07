import { useEffect, useRef, useState } from 'react'
import axios from 'axios'

/**
 * Botón "Iniciar sesión con Google".
 *
 * El Client ID lo pide en runtime a GET /api/config (única fuente de verdad: la env var
 * GOOGLE_CLIENT_ID del backend). Si no está configurado, el componente no renderiza nada
 * (tampoco el separador "o"), así que las páginas de login/registro quedan idénticas a hoy
 * hasta que se enchufe el Client ID.
 *
 * Props:
 *   - onCredential(credential): recibe el ID token de Google para mandarlo al backend.
 *   - onError(msg): opcional, para mostrar errores.
 *   - text: 'signin_with' | 'signup_with' | 'continue_with' (texto del botón de Google).
 *   - divider: si es true, dibuja un separador "o" arriba del botón.
 */
function GoogleLoginButton({ onCredential, onError, text = 'signin_with', divider = false }) {
  const divRef = useRef(null)
  // Guardamos los callbacks en un ref para no re-inicializar Google en cada render del padre.
  const cbRef = useRef({ onCredential, onError })
  cbRef.current = { onCredential, onError }

  // null = cargando config; '' = no configurado; string = Client ID.
  const [clientId, setClientId] = useState(null)

  useEffect(() => {
    let activo = true
    axios.get('/api/config')
      .then(res => { if (activo) setClientId(res.data?.googleClientId || '') })
      .catch(() => { if (activo) setClientId('') })
    return () => { activo = false }
  }, [])

  useEffect(() => {
    if (!clientId) return
    let cancelado = false

    // El script de GIS (index.html) es async: esperamos a que window.google esté listo.
    const render = () => {
      if (cancelado) return
      if (!window.google?.accounts?.id || !divRef.current) {
        setTimeout(render, 150)
        return
      }
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (resp) => {
          if (resp?.credential) cbRef.current.onCredential?.(resp.credential)
          else cbRef.current.onError?.('No se recibió la credencial de Google.')
        },
      })
      divRef.current.innerHTML = ''
      window.google.accounts.id.renderButton(divRef.current, {
        type: 'standard',
        theme: 'outline',
        size: 'large',
        text,          // signin_with / signup_with / continue_with
        shape: 'rectangular',
        logo_alignment: 'left',
        width: 320,
        locale: 'es',
      })
    }
    render()
    return () => { cancelado = true }
  }, [clientId, text])

  if (!clientId) return null // cargando o no configurado → no mostramos nada

  return (
    <>
      {divider && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '18px 0' }}>
          <div style={{ flex: 1, height: 1, background: '#e5e5e5' }} />
          <span style={{ fontSize: 12, color: '#999' }}>o</span>
          <div style={{ flex: 1, height: 1, background: '#e5e5e5' }} />
        </div>
      )}
      <div ref={divRef} style={{ display: 'flex', justifyContent: 'center' }} />
    </>
  )
}

export default GoogleLoginButton
