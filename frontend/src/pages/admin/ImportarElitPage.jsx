import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

function ImportarElitPage() {
  const [configurado, setConfigurado] = useState(null)   // null = cargando
  const [categoria, setCategoria] = useState('')
  const [marca, setMarca] = useState('')
  const [precioMinUsd, setPrecioMinUsd] = useState('')
  const [soloConStock, setSoloConStock] = useState(true)

  const [filtros, setFiltros] = useState(null)           // { categorias, marcas }
  const [cargandoFiltros, setCargandoFiltros] = useState(false)
  const [preview, setPreview] = useState(null)           // { total, muestra }
  const [previewLoading, setPreviewLoading] = useState(false)
  const [importando, setImportando] = useState(false)
  const [sincronizando, setSincronizando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/admin/elit/estado')
      .then(res => setConfigurado(!!res.data.configurado))
      .catch(() => setConfigurado(false))
  }, [])

  const cargarFiltros = async () => {
    setCargandoFiltros(true); setError('')
    try {
      const res = await axios.get('/api/admin/elit/filtros')
      setFiltros(res.data)
    } catch (e) {
      setError(e.response?.data?.error || 'No se pudieron cargar las categorías/marcas.')
    } finally {
      setCargandoFiltros(false)
    }
  }

  const previsualizar = async () => {
    setPreviewLoading(true); setError(''); setPreview(null); setResultado(null)
    try {
      const res = await axios.post('/api/admin/elit/previsualizar', { categoria, marca, store: 'all' })
      setPreview(res.data)
    } catch (e) {
      setError(e.response?.data?.error || 'Error al previsualizar.')
    } finally {
      setPreviewLoading(false)
    }
  }

  const importar = async () => {
    if (!window.confirm('¿Importar/sincronizar estos productos de Elit a tu catálogo?')) return
    setImportando(true); setError(''); setResultado(null)
    try {
      const res = await axios.post('/api/admin/elit/importar', { categoria, marca, soloConStock, store: 'all', precioMinUsd })
      setResultado(res.data)
    } catch (e) {
      setError(e.response?.data?.error || 'Error al importar. Revisá las credenciales o probá con menos productos.')
    } finally {
      setImportando(false)
    }
  }

  const sincronizar = async () => {
    setSincronizando(true); setError(''); setResultado(null)
    try {
      const res = await axios.post('/api/admin/elit/sincronizar')
      setResultado(res.data)
    } catch (e) {
      setError(e.response?.data?.error || 'Error al sincronizar.')
    } finally {
      setSincronizando(false)
    }
  }

  if (configurado === null) return (<div><h1>Importar de Elit</h1><div className="card"><p>Cargando...</p></div></div>)

  if (!configurado) return (
    <div>
      <h1>Importar de Elit</h1>
      <div className="card" style={{ borderLeft: '4px solid var(--color-danger)' }}>
        <h2 style={{ marginBottom: '8px' }}>API de Elit no configurada</h2>
        <p style={{ marginBottom: '10px' }}>Para activar la importación, cargá estas variables de entorno en Railway:</p>
        <pre style={{ background: '#16181d', color: '#e9eae5', padding: '12px 14px', borderRadius: '8px', fontSize: '13px', overflowX: 'auto' }}>
{`ELIT_USER_ID=tu_id_de_cliente
ELIT_TOKEN=tu_token_de_api`}
        </pre>
        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '10px' }}>
          Las conseguís en tu panel de Elit → Integración API. Después se redeploya solo.
        </p>
      </div>
    </div>
  )

  return (
    <div>
      <h1>Importar de Elit</h1>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '18px' }}>
        Traé productos del catálogo mayorista de Elit (con precios, stock e imágenes). Se crean/actualizan
        bajo el proveedor <strong>"Elit"</strong>; ajustá su margen y flete en <Link to="/admin/proveedores" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Proveedores</Link>.
      </p>

      <div className="card">
        <h2 style={{ marginBottom: '14px' }}>Filtros</h2>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Categoría (opcional)</label>
            <input list="elit-categorias" value={categoria} onChange={e => setCategoria(e.target.value)} placeholder="Todas — ej: Notebooks" />
            <datalist id="elit-categorias">
              {(filtros?.categorias || []).map(c => <option key={c} value={c} />)}
            </datalist>
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Marca (opcional)</label>
            <input list="elit-marcas" value={marca} onChange={e => setMarca(e.target.value)} placeholder="Todas — ej: ASUS" />
            <datalist id="elit-marcas">
              {(filtros?.marcas || []).map(m => <option key={m} value={m} />)}
            </datalist>
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Costo mínimo en USD (opcional)</label>
            <input type="number" min="0" step="1" value={precioMinUsd} onChange={e => setPrecioMinUsd(e.target.value)} placeholder="ej: 100" />
          </div>
        </div>

        <div style={{ marginTop: '14px', display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', cursor: 'pointer' }}>
            <input type="checkbox" checked={soloConStock} onChange={e => setSoloConStock(e.target.checked)} />
            Importar solo productos con stock
          </label>
          <button onClick={cargarFiltros} className="btn btn-secondary" disabled={cargandoFiltros} style={{ fontSize: '13px' }}>
            {cargandoFiltros ? 'Cargando opciones...' : (filtros ? '↻ Recargar categorías/marcas' : 'Ver categorías/marcas disponibles')}
          </button>
        </div>

        <div style={{ marginTop: '18px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button onClick={previsualizar} className="btn btn-secondary" disabled={previewLoading || importando}>
            {previewLoading ? 'Consultando...' : 'Previsualizar (ver cuántos)'}
          </button>
          <button onClick={importar} className="btn btn-primary" disabled={importando || previewLoading}>
            {importando ? 'Importando... (puede tardar)' : 'Importar a mi catálogo'}
          </button>
        </div>

        <p style={{ marginTop: '14px', fontSize: '13px', color: 'var(--color-text-muted)' }}>
          🔄 <strong>Sincronización automática:</strong> todos los días a las 06:30 se actualizan solos los precios y stock
          de lo ya importado. También podés forzarla ahora:
          <button onClick={sincronizar} className="btn btn-secondary" disabled={sincronizando || importando}
                  style={{ marginLeft: '10px', fontSize: '13px', padding: '6px 12px' }}>
            {sincronizando ? 'Sincronizando...' : 'Sincronizar ahora'}
          </button>
        </p>

        {error && <p style={{ marginTop: '14px', color: 'var(--color-danger)' }}>{error}</p>}

        {preview && (
          <div style={{ marginTop: '16px', background: 'var(--color-lime-tint)', borderRadius: '10px', padding: '12px 14px' }}>
            <strong>{preview.total}</strong> producto(s) coinciden con el filtro.
            {preview.muestra?.length > 0 && (
              <ul style={{ margin: '8px 0 0 18px', fontSize: '13px', color: '#4c5520' }}>
                {preview.muestra.map((n, i) => <li key={i}>{n}</li>)}
              </ul>
            )}
          </div>
        )}
      </div>

      {resultado && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-lime)' }}>
          <h2 style={{ marginBottom: '8px' }}>✅ Importación lista</h2>
          <p style={{ marginBottom: '10px' }}>{resultado.mensaje}</p>
          <p>
            <strong>{resultado.creados}</strong> nuevos · <strong>{resultado.actualizados}</strong> actualizados
            {resultado.salteadosSinStock > 0 && <> · {resultado.salteadosSinStock} sin stock</>}
            {resultado.salteadosPorPrecio > 0 && <> · {resultado.salteadosPorPrecio} bajo el mínimo</>}
          </p>
          <Link to="/admin/productos" className="btn btn-primary" style={{ marginTop: '14px', display: 'inline-block' }}>
            Ver productos
          </Link>
        </div>
      )}
    </div>
  )
}

export default ImportarElitPage
