import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/useAuth'
import toast from 'react-hot-toast'
import { Calendar, Music, Ticket, Minus, Plus, Loader2, Users } from 'lucide-react'

export default function EventoDetallePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated, usuario } = useAuth()
  const [evento, setEvento] = useState(null)
  const [secciones, setSecciones] = useState([])
  const [loading, setLoading] = useState(true)
  const [seleccion, setSeleccion] = useState({})
  const [creandoReserva, setCreandoReserva] = useState(false)

  useEffect(() => {
    Promise.all([
      api.get(`/eventos/${id}`),
      api.get(`/inventario/${id}/secciones`)
    ]).then(([evRes, invRes]) => {
      setEvento(evRes.data.datos)
      setSecciones(invRes.data.datos || [])
    }).catch(() => navigate('/'))
    .finally(() => setLoading(false))
  }, [id, navigate])

  const formatFecha = (fecha) => new Date(fecha).toLocaleDateString('es-GT', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
  })

  const cambiarCantidad = (seccionId, delta) => {
    setSeleccion(prev => {
      const actual = prev[seccionId] || 0
      const nueva = Math.max(0, actual + delta)
      return { ...prev, [seccionId]: nueva }
    })
  }

  const totalSeleccionado = () => {
    return secciones.reduce((total, inv) => {
      const sec = inv.seccion
      const cantidad = seleccion[sec.id] || 0
      return total + (cantidad * sec.precio)
    }, 0)
  }

  const cantidadTotal = () => Object.values(seleccion).reduce((a, b) => a + b, 0)

  const handleReservar = async () => {
    if (!isAuthenticated()) {
      toast.error('Debes iniciar sesion para reservar')
      navigate('/login')
      return
    }

    const items = secciones
      .filter(inv => (seleccion[inv.seccion.id] || 0) > 0)
      .map(inv => ({
        seccionId: inv.seccion.id,
        tipoAsiento: inv.seccion.tipo,
        cantidad: seleccion[inv.seccion.id]
      }))

    if (items.length === 0) {
      toast.error('Selecciona al menos un boleto')
      return
    }

    setCreandoReserva(true)
    try {
      const idempotencyKey = `reserva-${usuario.id}-${Date.now()}`
      const res = await api.post('/reservas', {
        eventoId: id,
        items
      }, {
        headers: { 'Idempotency-Key': idempotencyKey }
      })
      const reservaId = res.data.datos.id
      toast.success('Reserva creada - tienes 10 minutos para pagar')
      navigate(`/pago/${reservaId}`)
    } catch (err) {
      toast.error(err.response?.data?.mensaje || 'Error al crear la reserva')
    } finally {
      setCreandoReserva(false)
    }
  }

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <Loader2 className="animate-spin text-purple-400" size={48} />
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />

      <div className="bg-gradient-to-r from-purple-900 to-gray-900 py-16 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex items-center gap-4 mb-4">
            <div className="bg-purple-800 rounded-full p-4">
              <Music className="text-purple-200" size={40} />
            </div>
            <div>
              <span className={`text-xs px-2 py-1 rounded-full font-medium mb-2 inline-block ${
                evento?.estado === 'ACTIVO' ? 'bg-green-900 text-green-300' : 'bg-gray-700 text-gray-300'
              }`}>
                {evento?.estado}
              </span>
              <h1 className="text-4xl font-bold text-white">{evento?.nombre}</h1>
            </div>
          </div>
          <p className="text-gray-300 text-lg mt-4">{evento?.descripcion}</p>
          <div className="flex items-center gap-2 mt-4 text-gray-400">
            <Calendar size={16} />
            <span>{evento?.fechaEvento && formatFecha(evento.fechaEvento)}</span>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <h2 className="text-2xl font-bold text-white mb-6">Selecciona tus boletos</h2>
            <div className="space-y-4">
              {secciones.map(inv => {
                const sec = inv.seccion
                const disponibles = inv.asientosDisponibles ?? inv.totalAsientos
                const cantidad = seleccion[sec.id] || 0
                const agotado = disponibles === 0

                return (
                  <div key={sec.id} className={`bg-gray-900 border rounded-xl p-6 ${
                    agotado ? 'border-gray-700 opacity-60' : 'border-gray-800 hover:border-purple-500 transition'
                  }`}>
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-xl font-bold text-white">{sec.nombre}</h3>
                        <p className="text-gray-400 text-sm">{sec.tipo}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <Users size={14} className="text-gray-500" />
                          <span className="text-gray-400 text-sm">{disponibles} disponibles</span>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-2xl font-bold text-purple-400">Q{sec.precio}</p>
                        <p className="text-gray-500 text-sm">por boleto</p>
                      </div>
                    </div>

                    {!agotado && (
                      <div className="flex items-center gap-4 mt-4">
                        <button onClick={() => cambiarCantidad(sec.id, -1)}
                          className="bg-gray-800 hover:bg-gray-700 text-white rounded-lg p-2 transition disabled:opacity-50"
                          disabled={cantidad === 0}>
                          <Minus size={18} />
                        </button>
                        <span className="text-white font-bold text-xl w-8 text-center">{cantidad}</span>
                        <button onClick={() => cambiarCantidad(sec.id, 1)}
                          className="bg-gray-800 hover:bg-gray-700 text-white rounded-lg p-2 transition disabled:opacity-50"
                          disabled={cantidad >= Math.min(10, disponibles)}>
                          <Plus size={18} />
                        </button>
                        {cantidad > 0 && (
                          <span className="text-purple-400 text-sm ml-2">
                            Subtotal: Q{(cantidad * sec.precio).toFixed(2)}
                          </span>
                        )}
                      </div>
                    )}

                    {agotado && (
                      <div className="mt-4">
                        <span className="bg-red-900 text-red-300 text-sm px-3 py-1 rounded-full">AGOTADO</span>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>

          <div className="lg:col-span-1">
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 sticky top-24">
              <h3 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                <Ticket className="text-purple-400" size={20} />
                Tu seleccion
              </h3>

              {cantidadTotal() === 0 ? (
                <p className="text-gray-500 text-sm">Selecciona boletos para continuar</p>
              ) : (
                <div className="space-y-3 mb-4">
                  {secciones.filter(inv => (seleccion[inv.seccion.id] || 0) > 0).map(inv => (
                    <div key={inv.seccion.id} className="flex justify-between text-sm">
                      <span className="text-gray-300">{inv.seccion.nombre} x {seleccion[inv.seccion.id]}</span>
                      <span className="text-white">Q{(seleccion[inv.seccion.id] * inv.seccion.precio).toFixed(2)}</span>
                    </div>
                  ))}
                  <div className="border-t border-gray-700 pt-3 flex justify-between font-bold">
                    <span className="text-white">Total</span>
                    <span className="text-purple-400 text-xl">Q{totalSeleccionado().toFixed(2)}</span>
                  </div>
                </div>
              )}

              <button
                onClick={handleReservar}
                disabled={cantidadTotal() === 0 || creandoReserva}
                className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-gray-700 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-xl transition flex items-center justify-center gap-2 mt-4"
              >
                {creandoReserva ? (
                  <><Loader2 className="animate-spin" size={18} /> Creando reserva...</>
                ) : (
                  <><Ticket size={18} /> Reservar boletos</>
                )}
              </button>

              <p className="text-gray-500 text-xs text-center mt-3">
                Tienes 10 minutos para completar el pago
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
