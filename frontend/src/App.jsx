import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
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
import DepurarPage from './pages/admin/DepurarPage'
import PromocionesPage from './pages/admin/PromocionesPage'
import CatalogPage from './pages/public/CatalogPage'
import ArmaTuPcPage from './pages/public/ArmaTuPcPage'
import ProductDetailPage from './pages/public/ProductDetailPage'
import LandingPage from './pages/public/LandingPage'
import CartPage from './pages/public/CartPage'
import CheckoutPage from './pages/public/CheckoutPage'
import MisPedidosPage from './pages/public/MisPedidosPage'
import MisPuntosPage from './pages/public/MisPuntosPage'
import PedidoDetailPage from './pages/public/PedidoDetailPage'
import PagoResultadoPage from './pages/public/PagoResultadoPage'
import RutaPrivada from './auth/RutaPrivada'
import Analitica from './components/Analitica'
import TerminosPage from './pages/public/legal/TerminosPage'
import NuevaOrdenPage from './pages/admin/NuevaOrdenPage'
import ComprobantePage from './pages/admin/ComprobantePage'
import GarantiaPage from './pages/public/legal/GarantiaPage'
import PrivacidadPage from './pages/public/legal/PrivacidadPage'
import ArrepentimientoPage from './pages/public/legal/ArrepentimientoPage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage'
import ResetPasswordPage from './pages/auth/ResetPasswordPage'
import ActivateAccountPage from './pages/auth/ActivateAccountPage'
import ProtectedRoute from './auth/ProtectedRoute'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Analitica />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        {/* Direcciones del sorteo (ya no está en la web): siguen circulando en mensajes y
            anuncios viejos, así que llevan a la tienda en lugar de a una página vacía. */}
        <Route path="/sorteo" element={<Navigate to="/" replace />} />
        <Route path="/inicio" element={<Navigate to="/" replace />} />
        <Route path="/bases-y-condiciones" element={<Navigate to="/" replace />} />
        <Route path="/terminos" element={<TerminosPage />} />
        <Route path="/garantia" element={<GarantiaPage />} />
        <Route path="/privacidad" element={<PrivacidadPage />} />
        <Route path="/arrepentimiento" element={<ArrepentimientoPage />} />

        <Route element={<PublicLayout />}>
          <Route path="/catalogo" element={<CatalogPage />} />
          <Route path="/arma-tu-pc" element={<ArmaTuPcPage />} />
          <Route path="/producto/:id" element={<ProductDetailPage />} />
          {/* El carrito y el checkout son públicos: la sesión se pide recién al confirmar. */}
          <Route path="/carrito" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/mis-pedidos" element={<RutaPrivada><MisPedidosPage /></RutaPrivada>} />
          <Route path="/mis-puntos" element={<RutaPrivada><MisPuntosPage /></RutaPrivada>} />
          <Route path="/pedido/:numero" element={<RutaPrivada><PedidoDetailPage /></RutaPrivada>} />
          <Route path="/pago/resultado" element={<RutaPrivada><PagoResultadoPage /></RutaPrivada>} />
        </Route>

        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />
        <Route path="/recuperar" element={<ForgotPasswordPage />} />
        <Route path="/restablecer" element={<ResetPasswordPage />} />
        <Route path="/activar-cuenta" element={<ActivateAccountPage />} />

        {/* Fuera del layout del admin: es una hoja para imprimir o guardar en PDF. */}
        <Route path="/admin/pedidos/:numero/comprobante" element={<ProtectedRoute><ComprobantePage /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
          <Route index element={<Dashboard />} />
          <Route path="proveedores" element={<ProveedoresPage />} />
          <Route path="importar-elit" element={<ImportarElitPage />} />
          <Route path="importar-invid" element={<ImportarInvidPage />} />
          <Route path="cargar-json" element={<CargarJsonPage />} />
          <Route path="pedidos" element={<PedidosPage />} />
          <Route path="pedidos/nuevo" element={<NuevaOrdenPage />} />
          <Route path="productos" element={<ProductosPage />} />
          <Route path="categorias" element={<CategoriasPage />} />
          <Route path="depurar" element={<DepurarPage />} />
          <Route path="imagenes" element={<ImagesPage />} />
          <Route path="promociones" element={<PromocionesPage />} />
          <Route path="usuarios" element={<UsuariosPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
