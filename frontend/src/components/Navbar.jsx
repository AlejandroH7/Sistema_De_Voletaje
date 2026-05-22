import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { Ticket, LogOut, User, Settings } from 'lucide-react'

export default function Navbar() {
  const { usuario, logout, isAuthenticated, isAdmin } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-gray-900 border-b border-gray-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center gap-2">
            <Ticket className="text-purple-400" size={28} />
            <span className="text-xl font-bold text-white">SoldOut</span>
          </Link>

          <div className="flex items-center gap-4">
            {isAuthenticated() ? (
              <>
                <Link to="/mis-reservas" className="text-gray-300 hover:text-white transition text-sm">
                  Mis reservas
                </Link>
                {isAdmin() && (
                  <Link to="/admin" className="text-purple-400 hover:text-purple-300 transition text-sm flex items-center gap-1">
                    <Settings size={16} /> Admin
                  </Link>
                )}
                <div className="flex items-center gap-2 text-gray-300 text-sm">
                  <User size={16} />
                  <span>{usuario?.nombre}</span>
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1 text-gray-400 hover:text-red-400 transition text-sm"
                >
                  <LogOut size={16} /> Salir
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-gray-300 hover:text-white transition text-sm">
                  Iniciar sesion
                </Link>
                <Link to="/registro" className="bg-purple-600 hover:bg-purple-700 text-white text-sm px-4 py-2 rounded-lg transition">
                  Registrarse
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
