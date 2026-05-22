import { useState } from 'react'
import { AuthContext } from './AuthContextBase'

function obtenerUsuarioGuardado() {
  const storedUsuario = localStorage.getItem('usuario')
  return storedUsuario ? JSON.parse(storedUsuario) : null
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => obtenerUsuarioGuardado())
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [loading] = useState(false)

  const login = (tokenData, usuarioData) => {
    localStorage.setItem('token', tokenData)
    localStorage.setItem('usuario', JSON.stringify(usuarioData))
    setToken(tokenData)
    setUsuario(usuarioData)
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
    setToken(null)
    setUsuario(null)
  }

  const isAdmin = () => usuario?.rol === 'ADMIN'
  const isAuthenticated = () => !!token

  return (
    <AuthContext.Provider value={{ usuario, token, login, logout, isAdmin, isAuthenticated, loading }}>
      {children}
    </AuthContext.Provider>
  )
}
