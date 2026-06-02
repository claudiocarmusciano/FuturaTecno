function CatalogPage() {
  return (
    <div>
      <h1>Catálogo de Productos</h1>
      <div className="card">
        <p>Aquí se mostrarán los productos disponibles.</p>
        <p>Características:</p>
        <ul>
          <li>Grid de productos con imágenes</li>
          <li>Filtros por marca, almacenamiento, RAM y precio</li>
          <li>Precios en USD + equivalente en ARS</li>
          <li>ETA dinámica de entrega</li>
          <li>Página de detalle con especificaciones</li>
        </ul>
      </div>
    </div>
  )
}

export default CatalogPage
