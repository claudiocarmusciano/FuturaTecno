import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'

const VACIA = { titulo: '', texto: '', enlace: '', orden: 0, fechaInicio: '', fechaFin: '', activo: false, escritorio: null, movil: null, quitarMovil: false }

const validarImagen = (file, width, height, etiqueta) => new Promise((resolve, reject) => {
  if (!file) return resolve()
  if (!['image/jpeg', 'image/webp'].includes(file.type)) return reject(new Error(`${etiqueta}: solo JPG o WebP.`))
  if (file.size > 500 * 1024) return reject(new Error(`${etiqueta}: debe pesar como máximo 500 KB.`))
  const image = new Image()
  const url = URL.createObjectURL(file)
  image.onload = () => {
    URL.revokeObjectURL(url)
    image.width === width && image.height === height
      ? resolve()
      : reject(new Error(`${etiqueta}: debe medir exactamente ${width} × ${height} px; mide ${image.width} × ${image.height} px.`))
  }
  image.onerror = () => { URL.revokeObjectURL(url); reject(new Error(`${etiqueta}: no se pudo leer la imagen.`)) }
  image.src = url
})

function CampoImagen({ etiqueta, requerida, archivo, imagenActualUrl, dimensiones, onChange }) {
  const [previewNueva, setPreviewNueva] = useState('')

  useEffect(() => {
    if (!archivo) { setPreviewNueva(''); return undefined }
    const url = URL.createObjectURL(archivo)
    setPreviewNueva(url)
    return () => URL.revokeObjectURL(url)
  }, [archivo])

  const preview = previewNueva || imagenActualUrl
  const estado = archivo
    ? `Nueva imagen seleccionada: ${archivo.name}`
    : imagenActualUrl
      ? 'Imagen actualmente guardada'
      : 'Todavía no hay una imagen guardada'

  return <label className="promo-admin-image-field">
    <span>{etiqueta} {requerida ? '*' : '(opcional para reemplazar)'}</span>
    {preview && <img
      className="promo-admin-image-preview"
      src={preview}
      alt={`Vista previa de ${etiqueta.toLowerCase()}`}
    />}
    <small className={preview ? 'promo-admin-image-status' : 'promo-admin-image-status is-missing'}>{estado}</small>
    <input type="file" accept="image/jpeg,image/webp" onChange={e => onChange(e.target.files[0] || null)} />
    <small>{dimensiones} · JPG o WebP · máximo 500 KB</small>
  </label>
}

function PromocionesPage() {
  const [promociones, setPromociones] = useState([])
  const [form, setForm] = useState(VACIA)
  const [editando, setEditando] = useState(null)
  const [mensaje, setMensaje] = useState('')
  const [guardando, setGuardando] = useState(false)

  const cargar = async () => {
    try { setPromociones((await axios.get('/api/admin/promociones')).data) }
    catch (e) { setMensaje(e.response?.data?.error || 'No se pudo cargar el carrousel.') }
  }
  useEffect(() => { cargar() }, [])
  const activas = useMemo(() => promociones.filter(p => p.activo).length, [promociones])
  const promocionActual = useMemo(() => editando ? promociones.find(p => p.id === editando) : null, [editando, promociones])

  const editar = p => {
    setEditando(p.id)
    setForm({ titulo: p.titulo || '', texto: p.texto || '', enlace: p.enlace || '', orden: p.orden || 0,
      fechaInicio: p.fechaInicio?.slice(0, 16) || '', fechaFin: p.fechaFin?.slice(0, 16) || '', activo: p.activo,
      escritorio: null, movil: null, quitarMovil: false })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const cancelar = () => { setEditando(null); setForm(VACIA); setMensaje('') }
  const campo = (key, value) => setForm(prev => ({ ...prev, [key]: value }))

  const guardar = async e => {
    e.preventDefault(); setMensaje(''); setGuardando(true)
    try {
      if (!editando && !form.escritorio) throw new Error('Seleccioná la imagen de escritorio.')
      if (!form.movil && (!editando || !promocionActual?.imagenMovilUrl)) throw new Error('Seleccioná la imagen móvil.')
      await validarImagen(form.escritorio, 1600, 300, 'Imagen de escritorio')
      await validarImagen(form.movil, 1080, 608, 'Imagen móvil')
      const data = new FormData()
      ;['titulo', 'texto', 'enlace', 'orden', 'activo', 'quitarMovil'].forEach(k => data.append(k, form[k] ?? ''))
      if (form.fechaInicio) data.append('fechaInicio', form.fechaInicio)
      if (form.fechaFin) data.append('fechaFin', form.fechaFin)
      if (form.escritorio) data.append('escritorio', form.escritorio)
      if (form.movil) data.append('movil', form.movil)
      if (editando) await axios.put(`/api/admin/promociones/${editando}`, data)
      else await axios.post('/api/admin/promociones', data)
      cancelar(); await cargar(); setMensaje('Imagen guardada correctamente.')
    } catch (e2) { setMensaje(e2.response?.data?.error || e2.message || 'No se pudo guardar la imagen.') }
    finally { setGuardando(false) }
  }

  const eliminar = async p => {
    if (!window.confirm(`¿Eliminar la imagen “${p.titulo || `#${p.id}`}” del carrousel?`)) return
    try { await axios.delete(`/api/admin/promociones/${p.id}`); await cargar() }
    catch (e) { setMensaje(e.response?.data?.error || 'No se pudo eliminar.') }
  }

  return <div>
    <h1>Carrousel</h1>
    <div className="card">
      <strong>{activas}/10 imágenes activas</strong>
      <p style={{ color: 'var(--color-text-muted)', fontSize: 14, marginBottom: 0 }}>
        El carrousel se publica al tener al menos 4 imágenes activas y vigentes. Escritorio: 1600 × 300 px. Móvil: 1080 × 608 px. JPG o WebP, máximo 500 KB cada una.
      </p>
      {activas > 0 && activas < 4 && <p style={{ color: '#d7a928', fontWeight: 600 }}>Faltan {4 - activas} imágenes activas para publicar el carrousel.</p>}
    </div>

    <form className="card" onSubmit={guardar}>
      <h2>{editando ? 'Editar imagen' : 'Nueva imagen'}</h2>
      <div className="promo-admin-grid">
        <label>Título opcional<input value={form.titulo} maxLength={160} onChange={e => campo('titulo', e.target.value)} /></label>
        <label>Orden<input type="number" min="0" value={form.orden} onChange={e => campo('orden', e.target.value)} /></label>
        <label className="promo-admin-wide">Texto opcional<textarea rows="2" value={form.texto} maxLength={500} onChange={e => campo('texto', e.target.value)} /></label>
        <label className="promo-admin-wide">Enlace opcional<input value={form.enlace} placeholder="/catalogo?cat=71 o https://..." onChange={e => campo('enlace', e.target.value)} /></label>
        <label>Publicar desde<input type="datetime-local" value={form.fechaInicio} onChange={e => campo('fechaInicio', e.target.value)} /></label>
        <label>Publicar hasta<input type="datetime-local" value={form.fechaFin} onChange={e => campo('fechaFin', e.target.value)} /></label>
        <CampoImagen
          etiqueta="Imagen escritorio"
          requerida={!editando}
          archivo={form.escritorio}
          imagenActualUrl={promocionActual?.imagenEscritorioUrl}
          dimensiones="1600 × 300 px"
          onChange={file => campo('escritorio', file)}
        />
        <CampoImagen
          etiqueta="Imagen móvil"
          requerida={!editando || !promocionActual?.imagenMovilUrl}
          archivo={form.movil}
          imagenActualUrl={promocionActual?.imagenMovilUrl}
          dimensiones="1080 × 608 px"
          onChange={file => campo('movil', file)}
        />
      </div>
      <label style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 14 }}><input type="checkbox" checked={form.activo} onChange={e => campo('activo', e.target.checked)} /> Activa</label>
      <div style={{ display: 'flex', gap: 10, marginTop: 18 }}><button className="btn btn-primary" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>{editando && <button type="button" className="btn btn-secondary" onClick={cancelar}>Cancelar</button>}</div>
      {mensaje && <p style={{ marginBottom: 0 }}>{mensaje}</p>}
    </form>

    <div className="promo-admin-list">
      {promociones.map(p => <article className="card promo-admin-item" key={p.id}>
        <picture><source media="(max-width: 600px)" srcSet={p.imagenMovilUrl || p.imagenEscritorioUrl} /><img src={p.imagenEscritorioUrl} alt="" /></picture>
        <div><strong>{p.titulo || `Imagen #${p.id}`}</strong><p>{p.texto || 'Sin texto'}</p><small>Orden {p.orden} · {p.activo ? 'Activa' : 'Inactiva'}{p.fechaInicio ? ` · desde ${new Date(p.fechaInicio).toLocaleString('es-AR')}` : ''}{p.fechaFin ? ` · hasta ${new Date(p.fechaFin).toLocaleString('es-AR')}` : ''}</small></div>
        <div style={{ display: 'flex', gap: 8 }}><button className="btn btn-secondary" onClick={() => editar(p)}>Editar</button><button className="btn btn-secondary" onClick={() => eliminar(p)}>Eliminar</button></div>
      </article>)}
      {!promociones.length && <div className="card">Todavía no cargaste imágenes para el carrousel.</div>}
    </div>
  </div>
}

export default PromocionesPage
