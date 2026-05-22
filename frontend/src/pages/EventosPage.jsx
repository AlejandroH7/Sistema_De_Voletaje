import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import { Calendar, Music, Search, Loader2 } from 'lucide-react'

export default function EventosPage() {
  const [eventos, setEventos] = useState([])
  const [loading, setLoading] = useState(true)
  const [busqueda, setBusqueda] = useState('')

  useEffect(() => {
    api.get('/eventos')
      .then(res => setEventos(res.data.datos || []))
      .catch(() => setEventos([]))
      .finally(() => setLoading(false))
  }, [])

  const eventosFiltrados = eventos.filter(e =>
    e.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    e.descripcion?.toLowerCase().includes(busqueda.toLowerCase())
  )

  const formatFecha = (fecha) => {
    return new Date(fecha).toLocaleDateString('es-GT', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
    })
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />

      <div className="bg-gradient-to-r from-purple-900 to-gray-900 py-16 px-4">
        <div className="max-w-7xl mx-auto text-center">
          <h1 className="text-5xl font-bold text-white mb-4">Eventos disponibles</h1>
          <p className="text-gray-300 text-lg mb-8">Encuentra tu proximo evento y compra tus boletos</p>
          <div className="relative max-w-md mx-auto">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar eventos..."
              value={busqueda}
              onChange={e => setBusqueda(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 rounded-xl pl-10 pr-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-purple-500 transition"
            />
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-12">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="animate-spin text-purple-400" size={48} />
          </div>
        ) : eventosFiltrados.length === 0 ? (
          <div className="text-center py-20">
            <Music className="mx-auto text-gray-600 mb-4" size={64} />
            <p className="text-gray-400 text-xl">No hay eventos disponibles</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {eventosFiltrados.map(evento => (
              <Link key={evento.id} to={`/eventos/${evento.id}`}
                className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden hover:border-purple-500 hover:shadow-lg hover:shadow-purple-900/20 transition group">
                <div className="bg-gradient-to-br from-purple-800 to-gray-800 h-48 flex items-center justify-center">
                  <Music className="text-purple-300 opacity-50 group-hover:opacity-80 transition" size={80} />
                </div>
                <div className="p-6">
                  <div className="flex items-center gap-2 mb-2">
                    <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                      evento.estado === 'ACTIVO' ? 'bg-green-900 text-green-300' :
                      evento.estado === 'AGOTADO' ? 'bg-red-900 text-red-300' :
                      'bg-gray-700 text-gray-300'
                    }`}>
                      {evento.estado}
                    </span>
                    <span className="text-xs text-gray-500">{evento.tipoEvento}</span>
                  </div>
                  <h3 className="text-xl font-bold text-white mb-2 group-hover:text-purple-300 transition">
                    {evento.nombre}
                  </h3>
                  <p className="text-gray-400 text-sm mb-4 line-clamp-2">{evento.descripcion}</p>
                  <div className="space-y-2">
                    <div className="flex items-center gap-2 text-gray-400 text-sm">
                      <Calendar size={14} />
                      <span>{formatFecha(evento.fechaEvento)}</span>
                    </div>
                  </div>
                  <div className="mt-4 pt-4 border-t border-gray-800">
                    <span className="text-purple-400 text-sm font-medium group-hover:text-purple-300 transition">
                      Ver detalles -&gt;
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
