import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/useAuth'
import { Ticket, Clock, CheckCircle, XCircle, Ban, Loader2 } from 'lucide-react'

export default function MisReservasPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const [reservas, setReservas] = useState([])
  const [loading, setLoading] = useState(Boolean(usuario?.id))

  useEffect(() => {
    if (!usuario?.id) {
      return
    }

    api.get(`/reservas/usuario/${usuario.id}`)
      .then(res => setReservas(res.data.datos || []))
      .catch(() => setReservas([]))
      .finally(() => setLoading(false))
  }, [usuario?.id])

  const estadoConfig = {
    PENDIENTE: {
      color: 'bg-yellow-900 text-yellow-300',
      icon: <Clock size={16} />,
      label: 'Pendiente'
    },
    CONFIRMADO: {
      color: 'bg-green-900 text-green-300',
      icon: <CheckCircle size={16} />,
      label: 'Confirmado'
    },
    EXPIRADO: {
      color: 'bg-red-900 text-red-300',
      icon: <XCircle size={16} />,
      label: 'Expirado'
    },
    CANCELADO: {
      color: 'bg-gray-700 text-gray-300',
      icon: <Ban size={16} />,
      label: 'Cancelado'
    },
  }

  const formatFecha = (fecha) => {
    if (!fecha) return 'No disponible'

    return new Date(fecha).toLocaleDateString('es-GT', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />

      <div className="max-w-4xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-white mb-8 flex items-center gap-3">
          <Ticket className="text-purple-400" size={32} />
          Mis reservas
        </h1>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="animate-spin text-purple-400" size={48} />
          </div>
        ) : reservas.length === 0 ? (
          <div className="text-center py-20">
            <Ticket className="mx-auto text-gray-600 mb-4" size={64} />
            <p className="text-gray-400 text-xl mb-4">No tienes reservas aún</p>
            <button
              onClick={() => navigate('/')}
              className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-3 rounded-xl transition"
            >
              Ver eventos
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {reservas.map(reserva => {
              const config = estadoConfig[reserva.estado] || estadoConfig.CANCELADO

              return (
                <div
                  key={reserva.id}
                  className="bg-gray-900 border border-gray-800 rounded-2xl p-6 hover:border-purple-500 transition"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <div className="flex items-center gap-2 mb-2">
                        <span className={`flex items-center gap-1 text-xs px-3 py-1 rounded-full font-medium ${config.color}`}>
                          {config.icon} {config.label}
                        </span>
                      </div>
                      <p className="text-gray-500 text-xs font-mono">ID: {reserva.id}</p>
                    </div>

                    <div className="text-right">
                      <p className="text-2xl font-bold text-purple-400">
                        Q{reserva.precioTotal?.toFixed(2)}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    <div>
                      <p className="text-gray-500 mb-1">Creada</p>
                      <p className="text-gray-300">{formatFecha(reserva.creadoEn)}</p>
                    </div>

                    <div>
                      <p className="text-gray-500 mb-1">
                        {reserva.estado === 'CONFIRMADO' ? 'Confirmada' : 'Expira'}
                      </p>
                      <p className="text-gray-300">
                        {reserva.estado === 'CONFIRMADO'
                          ? formatFecha(reserva.confirmadoEn)
                          : formatFecha(reserva.expiraEn)}
                      </p>
                    </div>
                  </div>

                  {reserva.estado === 'PENDIENTE' && (
                    <div className="mt-4 pt-4 border-t border-gray-800">
                      <button
                        onClick={() => navigate(`/pago/${reserva.id}`)}
                        className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition"
                      >
                        Completar pago
                      </button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
