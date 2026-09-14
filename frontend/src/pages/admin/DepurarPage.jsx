import { useState } from 'react'
import axios from 'axios'
import { IconTrash } from '../../components/icons'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

const diasDesde = (iso) =>
  iso ? Math.floor((Date.now() - new Date(iso).getTime()) / 86400000) : null

// Elit e Invid se sincronizan solos todos los días: si aparecen acá es porque el mayorista dejó
// de ofrecerlos... o porque la sync se cortó. Conviene que el admin sepa distinguir los dos casos.
const MAYORISTAS = ['elit', 'invid']
const esMayorista = (proveedor) =>
  MAYORISTAS.some((m) => (proveedor || '').toLowerCase().includes(m))

function DepurarPage() {
  const [dias, setDias] = useState(3)
  const [productos, setProductos] = useState(null)   // null = todavía no se buscó
  const [diasBuscados, setDiasBuscados] = useState(3)
  const [buscando, setBuscando] = useState(false)
  const [purgando, setPurgando] = useState(false)
  const [seleccionados, setSeleccionados] = useState(new Set())
  const [mensaje, setMensaje] = useState('')

  const buscar = async () => {
    setBuscando(true)
    setMensaje('')
    try {
      const res = await axios.get('/api/admin/productos/vencidos', { params: { dias } })
      setProductos(res.data?.productos || [])
      setDiasBuscados(res.data?.dias ?? dias)
      setSeleccionados(new Set())
    } catch (e) {
      console.error(e)
      setMensaje('No se pudo obtener el listado: ' + (e.response?.data?.error || e.message))
    } finally {
      setBuscando(false)
    }
  }

  const alternar = (id) => {
    const s = new Set(seleccionados)
    s.has(id) ? s.delete(id) : s.add(id)
    setSeleccionados(s)
  }

  const seleccionarGrupo = (lista) => {
    const s = new Set(seleccionados)
    const todosPuestos = lista.every((p) => s.has(p.id))
    lista.forEach((p) => (todosPuestos ? s.delete(p.id) : s.add(p.id)))
    setSeleccionados(s)
  }

  const purgar = async () => {
    const cantidad = seleccionados.size
    if (!cantidad) return
    const ok = window.confirm(
      `Vas a sacar ${cantidad} producto(s) del catálogo.\n\n` +
      'No se borran: quedan dados de baja en la base y su imagen, categoría y medidas se guardan ' +
      'por marca y modelo, así que una futura alta los recupera sola.\n\n¿Confirmás?'
    )
    if (!ok) return
    setPurgando(true)
    setMensaje('')
    try {
      const res = await axios.post('/api/admin/productos/dar-de-baja', { ids: [...seleccionados] })
      setMensaje(res.data?.mensaje || `${cantidad} producto(s) dados de baja ✓`)
      setSeleccionados(new Set())
      await buscar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al dar de baja: ' + (e.response?.data?.message || e.message))
    } finally {
      setPurgando(false)
    }
  }

  // Agrupa por proveedor para que se vea de un vistazo de dónde viene lo vencido.
  const porProveedor = (productos || []).reduce((acc, p) => {
    const k = p.proveedor || 'Sin proveedor'
    ;(acc[k] = acc[k] || []).push(p)
    return acc
  }, {})
  const grupos = Object.entries(porProveedor).sort((a, b) => b[1].length - a[1].length)
  const hayMayoristas = grupos.some(([nombre]) => esMayorista(nombre))

  return (
    <div>
      <h1>Depurar catálogo</h1>
      <p style={{ color: 'var(--color-text-muted)', marginTop: '-8px', marginBottom: '20px' }}>
        Saca de la tienda los artículos que no se actualizan hace varios días. Primero mirás la lista,
        después confirmás.
      </p>

      {mensaje && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-lime)', color: 'var(--color-lime)' }}>
          {mensaje}
        </div>
      )}

      <div className="card" style={{ display: 'flex', gap: '12px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '13px' }}>
          Sin actualizarse hace más de
          <input
            type="number" min="1" value={dias}
            onChange={(e) => setDias(Math.max(1, Number(e.target.value) || 1))}
            style={{
              width: '90px', padding: '8px', border: '1px solid var(--color-border)',
              borderRadius: '4px', color: 'var(--color-text)'
            }}
          />
        </label>
        <span style={{ paddingBottom: '9px', color: 'var(--color-text-muted)' }}>días</span>
        <button onClick={buscar} className="btn btn-primary" disabled={buscando || purgando}>
          {buscando ? 'Buscando...' : 'Ver qué se sacaría'}
        </button>
      </div>

      {productos !== null && productos.length === 0 && (
        <div className="card">
          <p>No hay ningún producto sin actualizarse hace más de {diasBuscados} días. El catálogo está al día ✓</p>
        </div>
      )}

      {productos !== null && productos.length > 0 && (
        <>
          <div className="card">
            <h3 style={{ marginTop: 0 }}>
              {productos.length} producto(s) sin actualizarse hace más de {diasBuscados} días
            </h3>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', margin: '12px 0' }}>
              {grupos.map(([nombre, lista]) => (
                <button
                  key={nombre} onClick={() => seleccionarGrupo(lista)} className="btn btn-secondary"
                  style={{ fontSize: '13px' }} disabled={purgando}
                >
                  {nombre}: {lista.length}
                  {lista.every((p) => seleccionados.has(p.id)) ? ' ✓' : ''}
                </button>
              ))}
            </div>

            {hayMayoristas && (
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', lineHeight: 1.6 }}>
                ⚠️ Hay productos de Elit o Invid en la lista. Esos se sincronizan solos todos los días,
                así que aparecer acá significa que el mayorista dejó de ofrecerlos — o que la
                sincronización se cortó. Si son muchos y de golpe, revisá la sync antes de darlos de baja.
              </p>
            )}

            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '12px' }}>
              <button onClick={() => seleccionarGrupo(productos)} className="btn btn-secondary" disabled={purgando}>
                {productos.every((p) => seleccionados.has(p.id)) ? 'Deseleccionar todos' : `Seleccionar los ${productos.length}`}
              </button>
              <button onClick={purgar} className="btn-accion danger" disabled={purgando || seleccionados.size === 0}>
                <IconTrash /> {purgando ? 'Dando de baja...' : `Sacar del catálogo (${seleccionados.size})`}
              </button>
            </div>
          </div>

          <div className="card" style={{ overflowX: 'auto' }}>
            <table className="table">
              <thead>
                <tr>
                  <th style={{ width: '40px' }}></th>
                  <th>Producto</th>
                  <th>Categoría</th>
                  <th>Proveedor</th>
                  <th>Actualizado</th>
                </tr>
              </thead>
              <tbody>
                {productos.map((p) => (
                  <tr key={p.id} style={seleccionados.has(p.id) ? { background: 'rgba(200, 224, 72, 0.08)' } : undefined}>
                    <td>
                      <input
                        type="checkbox" checked={seleccionados.has(p.id)} onChange={() => alternar(p.id)}
                        disabled={purgando} style={{ width: '20px', height: '20px', cursor: 'pointer' }}
                      />
                    </td>
                    <td>
                      <strong>{p.marca} {p.modelo}</strong>
                      <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{p.sku}</div>
                    </td>
                    <td>{p.categoria || <span style={{ color: 'var(--color-text-muted)' }}>sin categoría</span>}</td>
                    <td>{p.proveedor || '—'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {formatFecha(p.ultimaActualizacion)}
                      <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                        hace {diasDesde(p.ultimaActualizacion)} días
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

export default DepurarPage
