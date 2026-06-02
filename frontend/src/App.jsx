import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AdminLayout from './components/layouts/AdminLayout'
import PublicLayout from './components/layouts/PublicLayout'
import Dashboard from './pages/admin/Dashboard'
import ProveedoresPage from './pages/admin/ProveedoresPage'
import ParsingPage from './pages/admin/ParsingPage'
import ImagesPage from './pages/admin/ImagesPage'
import CatalogPage from './pages/public/CatalogPage'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<PublicLayout />}>
          <Route index element={<CatalogPage />} />
        </Route>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="proveedores" element={<ProveedoresPage />} />
          <Route path="parsing" element={<ParsingPage />} />
          <Route path="imagenes" element={<ImagesPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
