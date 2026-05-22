import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/useAuth'
import toast from 'react-hot-toast'
import { CreditCard, Clock, Ticket, Loader2, CheckCircle, XCircle } from 'lucide-react'

export default function PagoPage() {
  const { reservaId } = useParams()
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const [reserva, setReserva] = useState(null)
  const [loading, setLoading] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [ttl, setTtl] = useState(600)
  const [metodoPago, setMetodoPago] = useState('TARJETA')

  useEffect(() => {
    api.get(`/reservas/${reservaId}`)
      .then(res => setReserva(res.data.datos))
      .catch(() => { toast.error('Reserva no encontrada'); navigate('/') })
      .finally(() => setLoading(false))
  }, [reservaId, navigate])

  useEffect(() => {
    if (!reserva) return
    const expira = new Date(reserva.expiraEn).getTime()
    const interval = setInterval(() => {
      const restante = Math.max(0, Math.floor((expira - Date.now()) / 1000))
      setTtl(restante)
      if (restante === 0) {
        clearInterval(interval)
        toast.error('La reserva expiró')
        navigate('/')
      }
    }, 1000)
    return () => clearInterval(interval)
  }, [reserva, navigate])

  const formatTtl = (segundos) => {
    const m = Math.floor(segundos / 60).toString().padStart(2, '0')
    const s = (segundos % 60).toString().padStart(2, '0')
    return `${m}:${s}`
  }

  const handlePagar = async () => {
    if (!usuario?.id) {
      toast.error('No se encontró el usuario autenticado')
      navigate('/login')
      return
    }

    if (!reserva) {
      toast.error('No se encontró la reserva')
      return
    }

    setProcesando(true)
    try {
      const idempotencyKey = `pago-${reservaId}-${Date.now()}`
      const res = await api.post('/pagos', {
        reserva_id: reservaId,
        usuario_id: usuario.id,
        monto: reserva.precioTotal,
        metodo_pago: metodoPago
      }, {
        headers: { 'Idempotency-Key': idempotencyKey }
      })
      setResultado(res.data.datos)
    } catch (err) {
      toast.error(err.response?.data?.mensaje || 'Error al procesar el pago')
    } finally {
      setProcesando(false)
    }
  }

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <Loader2 className="animate-spin text-purple-400" size={48} />
    </div>
  )

  if (resultado) return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        {resultado.estado === 'COMPLETADO' ? (
          <>
            <CheckCircle className="mx-auto text-green-400 mb-6" size={80} />
            <h1 className="text-4xl font-bold text-white mb-4">¡Pago exitoso!</h1>
            <p className="text-gray-300 text-lg mb-2">Tu reserva ha sido confirmada</p>
            <p className="text-gray-500 mb-8">Recibirás una notificación con los detalles</p>
            <div className="bg-gray-900 border border-green-800 rounded-2xl p-6 mb-8 text-left">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-400">ID de pago</span>
                <span className="text-white font-mono text-xs">{resultado.id}</span>
              </div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-400">Monto</span>
                <span className="text-green-400 font-bold">Q{resultado.monto}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Método</span>
                <span className="text-white">{resultado.metodoPago}</span>
              </div>
            </div>
            <button
              onClick={() => navigate('/mis-reservas')}
              className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl transition"
            >
              Ver mis reservas
            </button>
          </>
        ) : (
          <>
            <XCircle className="mx-auto text-red-400 mb-6" size={80} />
            <h1 className="text-4xl font-bold text-white mb-4">Pago fallido</h1>
            <p className="text-gray-300 text-lg mb-8">No se pudo procesar tu pago. Intenta nuevamente.</p>
            <button
              onClick={() => setResultado(null)}
              className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl transition"
            >
              Intentar de nuevo
            </button>
          </>
        )}
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-lg mx-auto px-4 py-12">

        <div className={`flex items-center justify-center gap-2 mb-8 p-4 rounded-xl ${
          ttl < 60 ? 'bg-red-900 border border-red-700' : 'bg-gray-900 border border-gray-800'
        }`}>
          <Clock className={ttl < 60 ? 'text-red-400' : 'text-purple-400'} size={20} />
          <span className={`text-2xl font-bold font-mono ${ttl < 60 ? 'text-red-400' : 'text-white'}`}>
            {formatTtl(ttl)}
          </span>
          <span className="text-gray-400 text-sm">para completar el pago</span>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mb-6">
          <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
            <Ticket className="text-purple-400" size={20} />
            Resumen de reserva
          </h2>
          {reserva?.detalles?.map((d, i) => (
            <div key={i} className="flex justify-between text-sm py-2 border-b border-gray-800">
              <span className="text-gray-300">{d.nombreSeccion} × {d.cantidad}</span>
              <span className="text-white">Q{(d.precioUnitario * d.cantidad).toFixed(2)}</span>
            </div>
          ))}
          <div className="flex justify-between font-bold mt-4">
            <span className="text-white">Total</span>
            <span className="text-purple-400 text-xl">Q{reserva?.precioTotal?.toFixed(2)}</span>
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mb-6">
          <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
            <CreditCard className="text-purple-400" size={20} />
            Método de pago
          </h2>
          <div className="grid grid-cols-3 gap-3">
            {['TARJETA', 'EFECTIVO', 'TRANSFERENCIA'].map(metodo => (
              <button
                key={metodo}
                onClick={() => setMetodoPago(metodo)}
                className={`py-3 px-4 rounded-xl text-sm font-medium transition border ${
                  metodoPago === metodo
                    ? 'bg-purple-600 border-purple-500 text-white'
                    : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-purple-500'
                }`}
              >
                {metodo}
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={handlePagar}
          disabled={procesando}
          className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-gray-700 disabled:cursor-not-allowed text-white font-bold py-4 rounded-xl transition flex items-center justify-center gap-2 text-lg"
        >
          {procesando ? (
            <><Loader2 className="animate-spin" size={20} /> Procesando pago...</>
          ) : (
            <><CreditCard size={20} /> Pagar Q{reserva?.precioTotal?.toFixed(2)}</>
          )}
        </button>

        <p className="text-center text-gray-500 text-xs mt-4">
          Pago simulado — 70% de probabilidad de éxito
        </p>
      </div>
    </div>
  )
}
