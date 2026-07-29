import { useState, useEffect, useCallback, Fragment } from 'react'
import axios from 'axios'

const inputEstilo = {
  padding: '8px 10px', fontSize: '14px', border: '1px solid var(--color-border)',
  borderRadius: '8px', color: 'var(--color-text)', background: 'var(--color-surface-2)'
}

function CategoriasPage() {
  const [arbol, setArbol] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [editando, setEditando] = useState(null)      // id en edición
  const [nombreEdit, setNombreEdit] = useState('')
  const [padreEdit, setPadreEdit] = useState('')      // '' = primer nivel

  const [creandoEn, setCreandoEn] = useState(null)    // id del padre, o 'raiz'
  const [nombreNuevo, setNombreNuevo] = useState('')

  const cargar = useCallback(() => {
    setCargando(true)
    axios.get('/api/admin/categorias')
      .then(res => setArbol(res.data))
      .catch(() => setError('No se pudieron cargar las categorías.'))
      .finally(() => setCargando(false))
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const raices = arbol.map(c => ({ id: c.id, nombre: c.nombre }))

  const manejarError = (err, fallback) => {
    setError(err.response?.data?.error || fallback)
    // El error viene del servidor con el detalle (ej. cuántos productos hay que reasignar),
    // así que se muestra tal cual en vez de un mensaje genérico.
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const crear = async (padreId) => {
    if (!nombreNuevo.trim()) return
    setError('')
    try {
      await axios.post('/api/admin/categorias', { nombre: nombreNuevo, padreId: padreId || null })
      setNombreNuevo(''); setCreandoEn(null); cargar()
    } catch (err) { manejarError(err, 'No se pudo crear la categoría.') }
  }

  const guardar = async (id) => {
    setError('')
    try {
      await axios.put(`/api/admin/categorias/${id}`, {
        nombre: nombreEdit,
        padreId: padreEdit === '' ? null : Number(padreEdit)
      })
      setEditando(null); cargar()
    } catch (err) { manejarError(err, 'No se pudo guardar el cambio.') }
  }

  const eliminar = async (cat) => {
    if (!window.confirm(`¿Borrar "${cat.nombre}"?`)) return
    setError('')
    try {
      await axios.delete(`/api/admin/categorias/${cat.id}`)
      cargar()
    } catch (err) { manejarError(err, 'No se pudo borrar la categoría.') }
  }

  const empezarEdicion = (cat) => {
    setEditando(cat.id)
    setNombreEdit(cat.nombre)
    setPadreEdit(cat.padreId == null ? '' : String(cat.padreId))
    setCreandoEn(null)
  }

  const fila = (cat, esSub) => (
    <div
      key={cat.id}
      style={{
        display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap',
        padding: '10px 0', borderBottom: '1px solid var(--color-border)',
        paddingLeft: esSub ? '26px' : 0
      }}
    >
      {editando === cat.id ? (
        <>
          <input
            value={nombreEdit}
            onChange={e => setNombreEdit(e.target.value)}
            style={{ ...inputEstilo, minWidth: '200px' }}
            autoFocus
          />
          {/* Mover de padre. Una categoría con subcategorías no puede pasar a ser subcategoría
              (sería un tercer nivel): el backend lo rechaza y el error se muestra arriba. */}
          <select value={padreEdit} onChange={e => setPadreEdit(e.target.value)} style={inputEstilo}>
            <option value="">— Categoría principal —</option>
            {raices.filter(r => r.id !== cat.id).map(r => (
              <option key={r.id} value={r.id}>Dentro de {r.nombre}</option>
            ))}
          </select>
          <button type="button" className="btn-accion" onClick={() => guardar(cat.id)}>Guardar</button>
          <button type="button" className="btn-accion" onClick={() => setEditando(null)}>Cancelar</button>
        </>
      ) : (
        <>
          <span style={{ fontWeight: esSub ? 500 : 600, fontSize: esSub ? '14px' : '15px' }}>
            {esSub && <span style={{ color: 'var(--color-text-muted)' }}>└ </span>}
            {cat.nombre}
          </span>
          <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
            {cat.cantidadProductos === 0
              ? 'sin productos'
              : `${cat.cantidadProductos} producto${cat.cantidadProductos === 1 ? '' : 's'}`}
          </span>
          <span style={{ marginLeft: 'auto', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            <button type="button" className="btn-accion" onClick={() => empezarEdicion(cat)}>Editar</button>
            {!esSub && (
              <button
                type="button"
                className="btn-accion"
                onClick={() => { setCreandoEn(cat.id); setNombreNuevo(''); setEditando(null) }}
              >
                + Subcategoría
              </button>
            )}
            <button type="button" className="btn-accion danger" onClick={() => eliminar(cat)}>Borrar</button>
          </span>
        </>
      )}
    </div>
  )

  const formNuevo = (padreId, etiqueta) => (
    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', padding: '10px 0', paddingLeft: padreId ? '26px' : 0 }}>
      <input
        value={nombreNuevo}
        onChange={e => setNombreNuevo(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter') crear(padreId) }}
        placeholder={etiqueta}
        style={{ ...inputEstilo, minWidth: '220px' }}
        autoFocus
      />
      <button type="button" className="btn-accion" onClick={() => crear(padreId)}>Crear</button>
      <button type="button" className="btn-accion" onClick={() => { setCreandoEn(null); setNombreNuevo('') }}>Cancelar</button>
    </div>
  )

  if (cargando) return (<div><h1>Categorías</h1><div className="card"><p>Cargando...</p></div></div>)

  return (
    <div>
      <h1>Categorías</h1>
      <p style={{ color: 'var(--color-text-muted)', fontSize: '14px', marginBottom: '18px' }}>
        Dos niveles: categorías principales y sus subcategorías. Los productos se asignan siempre
        a la hoja — una subcategoría, o una categoría principal que no tenga subcategorías.
      </p>

      {error && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-danger)', color: 'var(--color-danger)' }}>
          {error}
        </div>
      )}

      <div className="card">
        {arbol.map(raiz => (
          <Fragment key={raiz.id}>
            {fila(raiz, false)}
            {(raiz.hijos || []).map(h => fila(h, true))}
            {creandoEn === raiz.id && formNuevo(raiz.id, `Nueva subcategoría de ${raiz.nombre}`)}
          </Fragment>
        ))}

        {creandoEn === 'raiz'
          ? formNuevo(null, 'Nombre de la categoría')
          : (
            <button
              type="button"
              onClick={() => { setCreandoEn('raiz'); setNombreNuevo(''); setEditando(null) }}
              style={{
                marginTop: '16px', padding: '10px 20px', borderRadius: '8px', border: 'none',
                background: 'var(--color-lime)', color: '#16181d', fontWeight: 700, cursor: 'pointer'
              }}
            >
              + Nueva categoría
            </button>
          )}
      </div>
    </div>
  )
}

export default CategoriasPage
