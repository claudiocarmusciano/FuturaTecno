import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import { IconRefresh } from '../../components/icons'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function StatCard({ label, valor, sub, to }) {
  const contenido = (
    <div className="card" style={{ marginBottom: 0, height: '100%' }}>
      <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', fontWeight: 500, marginBottom: '8px' }}>{label}</div>
      <div style={{ fontSize: '34px', fontWeight: 600, letterSpacing: '-0.03em' }}>{valor}</div>
      {sub && <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '6px' }}>{sub}</div>}
    </div>
  )
  return to ? <Link to={to} style={{ textDecoration: 'none', color: 'inherit' }}>{contenido}</Link> : contenido
}

/**
 * Dispara el workflow de n8n que procesa las listas de proveedores de Notion en "Pendiente" y
 * muestra cómo va: el backend guarda cuándo se pidió y n8n le avisa al terminar con un resumen.
 * La URL del webhook es secreta y no pasa por el navegador. Notion sigue siendo la fuente de verdad.
 */
function ProcesarListasNotion({ proveedores }) {
  const [estado, setEstado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const [ahora, setAhora] = useState(Date.now())

  const consultar = () => axios.get('/api/admin/listas-notion/estado').then(r => setEstado(r.data)).catch(() => {})

  useEffect(() => { consultar() }, [])

  // Mientras procesa: consultar cada 5 s y refrescar el "hace X s".
  const procesando = estado?.estado === 'procesando'
  useEffect(() => {
    if (!procesando) return
    const t = setInterval(() => { consultar(); setAhora(Date.now()) }, 5000)
    return () => clearInterval(t)
  }, [procesando])

  const procesar = async () => {
    setEnviando(true)
    setError(null)
    try {
      await axios.post('/api/admin/listas-notion/procesar')
      await consultar()
      setAhora(Date.now())
    } catch (e) {
      setError(e.response?.data?.error || 'No se pudo contactar al servidor.')
    } finally {
      setEnviando(false)
    }
  }

  const nombreProveedor = id => proveedores.find(p => p.id === id)?.nombre || `Proveedor ${id}`
  const hora = iso => iso ? new Date(iso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' }) : ''
  const r = estado?.resultado
  const segundos = estado?.pedidoEn ? Math.max(0, Math.round((ahora - new Date(estado.pedidoEn).getTime()) / 1000)) : 0

  return (
    <div className="card" style={{ marginBottom: '18px' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ flex: '1 1 260px' }}>
          <h2 style={{ marginBottom: '4px' }}>Listas de proveedores</h2>
          <p style={{ color: 'var(--color-text-muted)', margin: 0, fontSize: '14px' }}>
            Procesa las páginas de Notion que estén en <strong>Pendiente</strong>. Podés seguir usando el panel mientras tanto.
          </p>
        </div>
        <button className="btn btn-primary" onClick={procesar} disabled={enviando || procesando}
          style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <IconRefresh className={procesando || enviando ? 'icono-girando' : ''} />
          {procesando ? 'Procesando…' : enviando ? 'Enviando…' : 'Procesar listas Notion'}
        </button>
      </div>

      {error && <p role="alert" style={{ margin: '12px 0 0', fontSize: '14px', color: 'var(--color-danger)' }}>{error}</p>}

      {procesando && (
        <p role="status" style={{ margin: '12px 0 0', fontSize: '14px', color: 'var(--color-lime)' }}>
          Procesando en n8n… empezó hace {segundos < 60 ? `${segundos} s` : `${Math.floor(segundos / 60)} min`}. Una lista chica tarda menos de un minuto.
        </p>
      )}

      {estado?.estado === 'sin_respuesta' && (
        <p role="status" style={{ margin: '12px 0 0', fontSize: '14px', color: '#f0c05a' }}>
          n8n no avisó que terminó (pedido a las {hora(estado.pedidoEn)}). Puede que no hubiera listas en Pendiente
          o que alguna haya fallado: revisá los estados en Notion.
        </p>
      )}

      {estado?.estado === 'terminado' && r && (
        <div role="status" style={{ marginTop: '12px', fontSize: '14px' }}>
          {r.listas === 0 ? (
            <p style={{ margin: 0, color: 'var(--color-text-muted)' }}>Última revisión a las {hora(estado.terminadoEn)}: no había listas en Pendiente.</p>
          ) : (
            <>
              <p style={{ margin: 0, color: 'var(--color-lime)' }}>
                Terminó a las {hora(estado.terminadoEn)} · {r.listas} lista(s) · {r.creados} creado(s) · {r.actualizados} actualizado(s)
                {r.revision > 0 && <> · <span style={{ color: '#f0c05a' }}>{r.revision} en revisión</span></>}
              </p>
              {Array.isArray(r.detalle) && (
                <ul style={{ margin: '8px 0 0', paddingLeft: '18px', color: 'var(--color-text-muted)' }}>
                  {r.detalle.map((d, i) => (
                    <li key={d.notionPageId || i}>
                      {nombreProveedor(d.proveedorId)}: {d.estadoFinal}
                      {/* Lo que hay que mirar primero va primero; las fotos faltantes son lo esperable. */}
                      {d.conProblemas > 0 && <> · <span style={{ color: '#f0c05a' }}>{d.conProblemas} para revisar</span></>}
                      {d.faltanFotos > 0 && ` · ${d.faltanFotos} sin foto`}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}

function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState({ productos: [], usuarios: [], proveedores: [], cotizacion: null, pendientes: [] })
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    Promise.all([
      axios.get('/api/admin/productos').then(r => r.data).catch(() => []),
      axios.get('/api/admin/usuarios').then(r => r.data).catch(() => []),
      axios.get('/api/admin/proveedores').then(r => r.data).catch(() => []),
      axios.get('/api/cotizacion').then(r => r.data).catch(() => null),
      axios.get('/api/admin/pedidos?estado=PENDIENTE').then(r => r.data).catch(() => []),
    ]).then(([productos, usuarios, proveedores, cotizacion, pendientes]) => {
      setData({ productos, usuarios, proveedores, cotizacion, pendientes })
    }).finally(() => setCargando(false))
  }, [])

  const { productos, usuarios, proveedores, cotizacion, pendientes } = data
  const sinImagen = productos.filter(p => !p.imagenUrl).length

  return (
    <div>
      <h1 style={{ marginBottom: '4px' }}>Hola, {user?.nombre || 'Administrador'}</h1>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '28px' }}>Resumen de tu tienda</p>

      <ProcesarListasNotion proveedores={proveedores} />

      {cargando ? (
        <div className="card"><p>Cargando métricas...</p></div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '18px', marginBottom: '18px' }}>
            <StatCard
              label="Pedidos pendientes"
              valor={pendientes.length}
              sub={pendientes.length > 0 ? 'Vencen a las 06:30' : 'Nada por atender'}
              to="/admin/pedidos"
            />
            <StatCard label="Productos publicados" valor={productos.length} to="/admin/productos" />
            <StatCard label="Clientes registrados" valor={usuarios.length} sub="Base de emails" to="/admin/usuarios" />
            <StatCard label="Proveedores" valor={proveedores.length} to="/admin/proveedores" />
            <StatCard
              label="Cotización del dólar"
              valor={cotizacion?.valor ? `$${formatNumber(cotizacion.valor)}` : '—'}
              sub="Usado para los precios"
            />
          </div>

          {sinImagen > 0 && (
            <Link to="/admin/imagenes" style={{ textDecoration: 'none' }}>
              <div className="card" style={{ marginBottom: '18px', borderLeft: '3px solid var(--color-accent)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text)' }}>
                  🖼️ Tenés <strong>{sinImagen}</strong> producto(s) sin imagen.
                </span>
                <span style={{ color: 'var(--color-accent)', fontSize: '14px', fontWeight: 500 }}>Buscar imágenes →</span>
              </div>
            </Link>
          )}

          <div className="card">
            <h2 style={{ marginBottom: '14px' }}>Últimos clientes registrados</h2>
            {usuarios.length === 0 ? (
              <p style={{ color: 'var(--color-text-muted)' }}>Todavía no hay clientes registrados.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr><th>Email</th><th>Nombre</th><th>Fecha</th></tr>
                </thead>
                <tbody>
                  {usuarios.slice(0, 5).map(u => (
                    <tr key={u.id}>
                      <td>{u.email}</td>
                      <td>{u.nombre || '—'}</td>
                      <td style={{ color: 'var(--color-text-muted)' }}>{formatFecha(u.fechaRegistro)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {usuarios.length > 5 && (
              <Link to="/admin/usuarios" style={{ fontSize: '14px', display: 'inline-block', marginTop: '12px' }}>Ver todos →</Link>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default Dashboard
