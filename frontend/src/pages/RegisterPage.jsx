import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'
import toast from 'react-hot-toast'
import { Ticket, Mail, Lock, User, Loader2 } from 'lucide-react'

export default function RegisterPage() {
  const [form, setForm] = useState({ nombre: '', email: '', contrasena: '' })
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await api.post('/auth/registro', { ...form, rol: 'CLIENTE' })
      toast.success('Cuenta creada exitosamente')
      navigate('/login')
    } catch (err) {
      toast.error(err.response?.data?.mensaje || 'Error al registrarse')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">

        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-2">
            <Ticket className="text-purple-400" size={40} />
            <h1 className="text-4xl font-bold text-white">SoldOut</h1>
          </div>
          <p className="text-gray-400">Crea tu cuenta y compra tus boletos</p>
        </div>

        <div className="bg-gray-800 bg-opacity-80 backdrop-blur rounded-2xl shadow-2xl p-8 border border-gray-700">
          <h2 className="text-2xl font-bold text-white mb-6">Crear cuenta</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Nombre completo</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="text"
                  placeholder="Tu nombre"
                  value={form.nombre}
                  onChange={e => setForm({...form, nombre: e.target.value})}
                  className="w-full bg-gray-700 border border-gray-600 rounded-lg pl-10 pr-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="email"
                  placeholder="tu@email.com"
                  value={form.email}
                  onChange={e => setForm({...form, email: e.target.value})}
                  className="w-full bg-gray-700 border border-gray-600 rounded-lg pl-10 pr-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Contraseña</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="password"
                  placeholder="Mínimo 6 caracteres"
                  value={form.contrasena}
                  onChange={e => setForm({...form, contrasena: e.target.value})}
                  className="w-full bg-gray-700 border border-gray-600 rounded-lg pl-10 pr-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition"
                  required
                  minLength={6}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-purple-800 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition flex items-center justify-center gap-2 mt-2"
            >
              {loading ? (
                <><Loader2 className="animate-spin" size={18} /> Creando cuenta...</>
              ) : (
                'Crear cuenta'
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-gray-400">
              ¿Ya tienes cuenta?{' '}
              <Link to="/login" className="text-purple-400 hover:text-purple-300 font-medium transition">
                Inicia sesión
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
