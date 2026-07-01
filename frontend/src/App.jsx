import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AdminLayout from './components/layouts/AdminLayout'
import PublicLayout from './components/layouts/PublicLayout'
import Dashboard from './pages/admin/Dashboard'
import ProveedoresPage from './pages/admin/ProveedoresPage'
import ParsingPage from './pages/admin/ParsingPage'
import ProductosPage from './pages/admin/ProductosPage'
import ImagesPage from './pages/admin/ImagesPage'
import ImportarElitPage from './pages/admin/ImportarElitPage'
import ImportarInvidPage from './pages/admin/ImportarInvidPage'
import UsuariosPage from './pages/admin/UsuariosPage'
import CatalogPage from './pages/public/CatalogPage'
import ProductDetailPage from './pages/public/ProductDetailPage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ProtectedRoute from './auth/ProtectedRoute'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<PublicLayout />}>
          <Route index element={<CatalogPage />} />
          <Route path="producto/:id" element={<ProductDetailPage />} />
        </Route>

        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />

        <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
          <Route index element={<Dashboard />} />
          <Route path="proveedores" element={<ProveedoresPage />} />
          <Route path="parsing" element={<ParsingPage />} />
          <Route path="importar-elit" element={<ImportarElitPage />} />
          <Route path="importar-invid" element={<ImportarInvidPage />} />
          <Route path="productos" element={<ProductosPage />} />
          <Route path="imagenes" element={<ImagesPage />} />
          <Route path="usuarios" element={<UsuariosPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
