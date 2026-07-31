import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AdminLayout from './components/layouts/AdminLayout'
import PublicLayout from './components/layouts/PublicLayout'
import Dashboard from './pages/admin/Dashboard'
import ProveedoresPage from './pages/admin/ProveedoresPage'
import ProductosPage from './pages/admin/ProductosPage'
import ImagesPage from './pages/admin/ImagesPage'
import ImportarElitPage from './pages/admin/ImportarElitPage'
import ImportarInvidPage from './pages/admin/ImportarInvidPage'
import CargarJsonPage from './pages/admin/CargarJsonPage'
import UsuariosPage from './pages/admin/UsuariosPage'
import PedidosPage from './pages/admin/PedidosPage'
import CategoriasPage from './pages/admin/CategoriasPage'
import CatalogPage from './pages/public/CatalogPage'
import ProductDetailPage from './pages/public/ProductDetailPage'
import LandingPage from './pages/public/LandingPage'
import CartPage from './pages/public/CartPage'
import CheckoutPage from './pages/public/CheckoutPage'
import MisPedidosPage from './pages/public/MisPedidosPage'
import PedidoDetailPage from './pages/public/PedidoDetailPage'
import RutaPrivada from './auth/RutaPrivada'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage'
import ResetPasswordPage from './pages/auth/ResetPasswordPage'
import ProtectedRoute from './auth/ProtectedRoute'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* La home es la landing (tiene su propio header/footer). El catálogo vive en /catalogo. */}
        <Route path="/" element={<LandingPage />} />

        <Route element={<PublicLayout />}>
          <Route path="/catalogo" element={<CatalogPage />} />
          <Route path="/producto/:id" element={<ProductDetailPage />} />
          {/* El carrito y el checkout son públicos: la sesión se pide recién al confirmar. */}
          <Route path="/carrito" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/mis-pedidos" element={<RutaPrivada><MisPedidosPage /></RutaPrivada>} />
          <Route path="/pedido/:numero" element={<RutaPrivada><PedidoDetailPage /></RutaPrivada>} />
        </Route>

        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />
        <Route path="/recuperar" element={<ForgotPasswordPage />} />
        <Route path="/restablecer" element={<ResetPasswordPage />} />

        <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
          <Route index element={<Dashboard />} />
          <Route path="proveedores" element={<ProveedoresPage />} />
          <Route path="importar-elit" element={<ImportarElitPage />} />
          <Route path="importar-invid" element={<ImportarInvidPage />} />
          <Route path="cargar-json" element={<CargarJsonPage />} />
          <Route path="pedidos" element={<PedidosPage />} />
          <Route path="productos" element={<ProductosPage />} />
          <Route path="categorias" element={<CategoriasPage />} />
          <Route path="imagenes" element={<ImagesPage />} />
          <Route path="usuarios" element={<UsuariosPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
