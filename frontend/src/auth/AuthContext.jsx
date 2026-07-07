import { createContext, useContext, useState, useEffect } from 'react'
import axios from 'axios'

const AuthContext = createContext(null)

// Aplica (o quita) el token al header de axios para todas las requests.
const aplicarToken = (token) => {
  if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete axios.defaults.headers.common['Authorization']
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [listo, setListo] = useState(false)

  // Al cargar la app, recupera la sesión de localStorage.
  useEffect(() => {
    const guardado = localStorage.getItem('auth')
    if (guardado) {
      try {
        const data = JSON.parse(guardado)
        aplicarToken(data.token)
        setUser(data)
      } catch { localStorage.removeItem('auth') }
    }
    setListo(true)
  }, [])

  const guardarSesion = (data) => {
    localStorage.setItem('auth', JSON.stringify(data))
    aplicarToken(data.token)
    setUser(data)
  }

  const login = async (email, password) => {
    const res = await axios.post('/api/auth/login', { email, password })
    guardarSesion(res.data)
    return res.data
  }

  const register = async (email, password, nombre) => {
    const res = await axios.post('/api/auth/register', { email, password, nombre })
    guardarSesion(res.data)
    return res.data
  }

  // Login con Google: recibe el ID token (credential) que emite Google Identity Services.
  const loginConGoogle = async (credential) => {
    const res = await axios.post('/api/auth/google', { credential })
    guardarSesion(res.data)
    return res.data
  }

  const logout = () => {
    localStorage.removeItem('auth')
    aplicarToken(null)
    setUser(null)
  }

  const value = {
    user,
    listo,
    isAdmin: user?.rol === 'ADMIN',
    isAuth: !!user,
    login,
    register,
    loginConGoogle,
    logout
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
