import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    const storedUsuario = localStorage.getItem('usuario')
    if (storedToken && storedUsuario) {
      setToken(storedToken)
      setUsuario(JSON.parse(storedUsuario))
    }
    setLoading(false)
  }, [])

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

export function useAuth() {
  return useContext(AuthContext)
}
