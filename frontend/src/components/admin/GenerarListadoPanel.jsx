import { useEffect, useRef, useState } from 'react'
import axios from 'axios'

const campo = { width: '100%', minWidth: 0, padding: '8px', border: '1px solid var(--color-border)', borderRadius: '6px', color: 'var(--color-text)', background: 'var(--color-surface-2)' }
const clavesSpecs = ['procesador', 'ram', 'almacenamiento', 'pantalla', 'gpu', 'sistema_operativo', 'otros']
const clave = a => `${a.marca.trim().toLowerCase()}|${a.modelo.trim().toLowerCase().replace(/\s+/g, ' ')}`

export default function GenerarListadoPanel({ onImportar, importando, proveedorId, errorImportacion }) {
  const [texto, setTexto] = useState('')
  const [articulos, setArticulos] = useState([])
  const [avisos, setAvisos] = useState([])
  const [error, setError] = useState('')
  const [etapa, setEtapa] = useState('')
  const [procesados, setProcesados] = useState(0)
  const [pagina, setPagina] = useState(0)
  const controller = useRef(null)
  useEffect(() => () => controller.current?.abort(), [])

  const generar = async (desdeNotion = false) => {
    const abort = new AbortController()
    controller.current = abort
    setError(''); setEtapa(desdeNotion ? 'notion' : 'generando')
    try {
      let entrada = texto
      if (desdeNotion) {
        const respuesta = await axios.get('/api/admin/carga-json/notion', { signal: abort.signal, timeout: 60000 })
        entrada = respuesta.data.texto
        setTexto(entrada)
      }
      setAvisos([]); setArticulos([]); setPagina(0); setProcesados(0); setEtapa('generando')
      const { data } = await axios.post('/api/admin/carga-json/generar', { texto: entrada }, { signal: abort.signal, timeout: 100000 })
      const lista = data.articulos
      setArticulos(lista); setAvisos(data.avisos || [])
      setEtapa('imagenes')
      const solicitudes = new Map()
      for (let inicio = 0; inicio < lista.length && !abort.signal.aborted; inicio += 3) {
        await Promise.all(lista.slice(inicio, inicio + 3).map(async (a, offset) => {
          try {
            const k = clave(a)
            if (!solicitudes.has(k)) solicitudes.set(k, axios.post('/api/admin/carga-json/generar-imagen', { marca: a.marca, modelo: a.modelo }, { signal: abort.signal, timeout: 45000 }))
            const res = await solicitudes.get(k)
            if (!abort.signal.aborted) setArticulos(prev => prev.map((item, i) => i === inicio + offset ? { ...item, imagenes: res.data.imagenes } : item))
          } catch (e) {
            if (!abort.signal.aborted) setAvisos(prev => [...prev, `No se pudo verificar la imagen de ${a.marca} ${a.modelo}. Podés cargarla manualmente.`])
          } finally {
            if (!abort.signal.aborted) setProcesados(n => n + 1)
          }
        }))
      }
    } catch (e) {
      if (!abort.signal.aborted) setError(e.response?.data?.error || 'No se pudo generar el listado. Intentá nuevamente.')
    } finally {
      if (controller.current === abort) { setEtapa(''); controller.current = null }
    }
  }

  const editar = (index, key, valor) => setArticulos(prev => prev.map((a, i) => i === index ? { ...a, [key]: valor, ...(['marca', 'modelo'].includes(key) ? { imagenes: [] } : {}) } : a))
  const eliminar = index => { setArticulos(prev => prev.filter((_, i) => i !== index)); setPagina(0) }
  const vistos = new Set()
  const duplicados = new Set()
  for (const a of articulos) { const k = clave(a); if (vistos.has(k)) duplicados.add(k); vistos.add(k) }
  const filasInvalidas = articulos.flatMap((a, i) => ( !a.marca.trim() || !a.modelo.trim() || a.modelo.length > 255 || a.marca.length > 255 || !Number.isFinite(Number(a.precio_usd)) || Number(a.precio_usd) <= 0 || Object.values(a.especificaciones || {}).join(' · ').length >= 500 || a.imagenes.some(u => !/^https?:\/\/\S+$/i.test(u) || u.length > 1000)) ? [i + 1] : [])
  const invalidos = filasInvalidas.length > 0
  const ocupado = Boolean(etapa) || importando
  const descargar = () => {
    const contenido = articulos.map(a => ({ ...a, precio_usd: Number(a.precio_usd) }))
    const url = URL.createObjectURL(new Blob([JSON.stringify(contenido, null, 2)], { type: 'application/json' }))
    const link = document.createElement('a'); link.href = url; link.download = 'articulos.json'; link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  return <div className="card" style={{ marginBottom: 18, minWidth: 0 }}>
    <h2 style={{ marginTop: 0 }}>Generar desde listado</h2>
    <p style={{ color: 'var(--color-text-muted)' }}>Cargar desde Notion trae el texto, genera los artículos y busca sus imágenes. Si pegás o editás texto manualmente, usá Generar artículos. Después revisá y pulsá Confirmar e importar.</p>
    <p style={{ color: 'var(--color-text-muted)' }}>La carga desde Notion reemplaza este borrador. El texto queda en esta pantalla aunque después lo borres de Notion; descargá el JSON o importalo antes de salir.</p>
    <label htmlFor="listado-texto">Listado de artículos</label>
    <textarea id="listado-texto" value={texto} onChange={e => setTexto(e.target.value)} disabled={ocupado} maxLength={20000} rows={8} style={{ ...campo, resize: 'vertical', display: 'block', marginTop: 8 }} placeholder={'ASUS\nASUS Vivobook 15 Core 7 150U 16GB, 512GB FHD Touch USD 1.020'} />
    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center', marginTop: 12 }}>
      <button className="btn btn-secondary" onClick={() => generar(true)} disabled={ocupado}>{etapa === 'notion' ? 'Leyendo Notion…' : 'Cargar desde Notion'}</button>
      <button className="btn btn-primary" onClick={() => generar()} disabled={ocupado || !texto.trim()}>{etapa === 'generando' ? 'Generando…' : 'Generar artículos'}</button>
      {etapa && <button className="btn btn-secondary" onClick={() => controller.current?.abort()}>{etapa === 'imagenes' ? 'Detener búsqueda y revisar' : 'Cancelar'}</button>}
      <span role="status" aria-live="polite">{etapa === 'imagenes' ? `Verificando imágenes: ${procesados} de ${articulos.length}` : `${texto.length.toLocaleString('es-AR')} / 20.000 caracteres`}</span>
    </div>
    {error && <p role="alert" style={{ color: 'var(--color-danger)' }}>{error}</p>}
    {avisos.length > 0 && <details style={{ marginTop: 16 }}><summary>Avisos del listado ({avisos.length})</summary><ul>{avisos.map((a, i) => <li key={i}>{a}</li>)}</ul></details>}
    {articulos.length > 0 && <>
      <h3>Revisar {articulos.length} artículos</h3>
      <p style={{ color: 'var(--color-text-muted)' }}>Podés corregir cada campo o quitar artículos. Las filas sin imagen también pueden importarse.</p>
      {duplicados.size > 0 && <p role="alert" style={{ color: 'var(--color-danger)' }}>Hay artículos con la misma marca y modelo. Eliminá las filas repetidas o diferenciá las variantes antes de importar.</p>}
      {invalidos && <p role="alert" style={{ color: 'var(--color-danger)' }}>Revisá marcas, modelos, precios positivos, URLs y especificaciones (menos de 500 caracteres).</p>}
      <div style={{ overflowX: 'auto', maxWidth: '100%' }}>
        <table className="table" style={{ minWidth: 960 }}>
          <thead><tr><th>Marca</th><th>Modelo y variante</th><th>USD</th><th>Categoría</th><th>Imagen</th><th>Detalles</th></tr></thead>
          <tbody>{articulos.slice(pagina * 15, pagina * 15 + 15).map((a, offset) => {
            const i = pagina * 15 + offset
            return <tr key={i} style={duplicados.has(clave(a)) ? { background: 'var(--color-danger-bg)' } : undefined}>
              <td><input aria-label={`Marca ${i + 1}`} style={{ ...campo, minWidth: 100 }} value={a.marca} disabled={ocupado} onChange={e => editar(i, 'marca', e.target.value)} /></td>
              <td><textarea aria-label={`Modelo ${i + 1}`} style={{ ...campo, minWidth: 240 }} value={a.modelo} rows={3} disabled={ocupado} onChange={e => editar(i, 'modelo', e.target.value)} /></td>
              <td><input aria-label={`Precio USD ${i + 1}`} type="number" min="0.01" step="0.01" style={{ ...campo, minWidth: 100 }} value={a.precio_usd} disabled={ocupado} onChange={e => editar(i, 'precio_usd', e.target.value)} /></td>
              <td><input aria-label={`Categoría ${i + 1}`} style={{ ...campo, minWidth: 160 }} value={a.categoria || ''} disabled={ocupado} onChange={e => editar(i, 'categoria', e.target.value)} /></td>
              <td>
                {a.imagenes[0] ? <img key={a.imagenes[0]} src={a.imagenes[0]} alt={`Imagen de ${a.modelo}`} referrerPolicy="no-referrer" style={{ width: 65, height: 65, objectFit: 'contain' }} onError={e => { e.currentTarget.style.display = 'none' }} /> : <span>Sin imagen</span>}
                <input aria-label={`URL imagen ${i + 1}`} placeholder="https://…" style={{ ...campo, minWidth: 200 }} value={a.imagenes[0] || ''} disabled={ocupado} onChange={e => editar(i, 'imagenes', e.target.value.trim() ? [e.target.value.trim()] : [])} />
              </td>
              <td><details><summary>Especificaciones</summary>{clavesSpecs.map(k => <label key={k} style={{ display: 'block', marginTop: 5 }}>{k.replace('_', ' ')}<input style={campo} value={a.especificaciones?.[k] || ''} disabled={ocupado} onChange={e => editar(i, 'especificaciones', { ...a.especificaciones, [k]: e.target.value })} /></label>)}</details>
                <button className="btn btn-secondary" style={{ marginTop: 8 }} onClick={() => eliminar(i)} disabled={ocupado}>Quitar</button></td>
            </tr>
          })}</tbody>
        </table>
      </div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center', marginTop: 16 }}>
        <button className="btn btn-secondary" onClick={() => setPagina(p => p - 1)} disabled={pagina === 0}>Anterior</button>
        <span>Página {pagina + 1} de {Math.ceil(articulos.length / 15)}</span>
        <button className="btn btn-secondary" onClick={() => setPagina(p => p + 1)} disabled={(pagina + 1) * 15 >= articulos.length}>Siguiente</button>
      </div>
      <div role="status" aria-live="polite" style={{ marginTop: 16 }}>
        {etapa === 'imagenes' && <p>Buscando imágenes: {procesados} de {articulos.length}. Podés esperar o <button className="btn btn-secondary" onClick={() => controller.current?.abort()}>Detener búsqueda y revisar</button>.</p>}
        {etapa && etapa !== 'imagenes' && <p>{etapa === 'notion' ? 'Leyendo Notion…' : 'Generando artículos…'}</p>}
        {importando && <p>Importando artículos. Esperá la confirmación del servidor.</p>}
        {duplicados.size > 0 && <p>Hay marcas y modelos repetidos. Revisá las filas resaltadas antes de importar.</p>}
        {invalidos && <p>Hay datos inválidos en las filas {filasInvalidas.join(', ')}. Revisá marca, modelo, precio, URL y especificaciones. <button className="btn btn-secondary" onClick={() => setPagina(Math.floor((filasInvalidas[0] - 1) / 15))}>Ver primera fila con error</button></p>}
      </div>
      {errorImportacion && <p role="alert" style={{ color: 'var(--color-danger)' }}>No se pudo importar: {typeof errorImportacion === 'string' ? errorImportacion : 'Revisá los datos e intentá nuevamente.'}</p>}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center', marginTop: 16 }}>
        <button className="btn btn-primary" disabled={ocupado || invalidos || duplicados.size > 0 || !proveedorId} onClick={() => onImportar(articulos.map(a => ({ ...a, precio_usd: Number(a.precio_usd) })))}>{importando ? 'Importando…' : 'Confirmar e importar'}</button>
        <button className="btn btn-secondary" disabled={ocupado || invalidos || duplicados.size > 0} onClick={descargar}>Descargar JSON</button>
        {!proveedorId && <span>Elegí un proveedor para importar.</span>}
      </div>
    </>}
  </div>
}
