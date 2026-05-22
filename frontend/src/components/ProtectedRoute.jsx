import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()

  if (loading) return <div className="flex items-center justify-center h-screen">Cargando...</div>
  if (!isAuthenticated()) return <Navigate to="/login" />
  if (adminOnly && !isAdmin()) return <Navigate to="/" />

  return children
}
