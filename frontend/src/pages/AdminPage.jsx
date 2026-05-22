import { useState, useEffect } from 'react'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'
import { Settings, Plus, MapPin, Music, Loader2, CheckCircle } from 'lucide-react'

export default function AdminPage() {
  const [tab, setTab] = useState('eventos')
  const [eventos, setEventos] = useState([])
  const [loading, setLoading] = useState(true)
  const [lugarForm, setLugarForm] = useState({ nombre: '', direccion: '', ciudad: 'Guatemala', capacidadMaxima: 50000 })
  const [eventoForm, setEventoForm] = useState({ nombre: '', descripcion: '', lugarId: '', fechaEvento: '', tipoEvento: 'MIXTO' })
  const [secciones, setSecciones] = useState([{ nombre: 'General', tipo: 'GENERAL', capacidad: 1000, precio: 150 }])
  const [lugares, setLugares] = useState([])
  const [creandoLugar, setCreandoLugar] = useState(false)
  const [creandoEvento, setCreandoEvento] = useState(false)

  useEffect(() => {
    Promise.all([
      api.get('/eventos'),
      api.get('/lugares').catch(() => ({ data: { datos: [] } }))
    ]).then(([evRes, luRes]) => {
      setEventos(evRes.data.datos || [])
      setLugares(luRes.data.datos || [])
    }).finally(() => setLoading(false))
  }, [])

  const handleCrearLugar = async (e) => {
    e.preventDefault()
    setCreandoLugar(true)
    try {
      const res = await api.post('/lugares', lugarForm)
      setLugares(prev => [...prev, res.data.datos])
      toast.success('Lugar creado exitosamente')
      setLugarForm({ nombre: '', direccion: '', ciudad: 'Guatemala', capacidadMaxima: 50000 })
    } catch (err) {
      toast.error(err.response?.data?.mensaje || 'Error al crear lugar')
    } finally {
      setCreandoLugar(false)
    }
  }

  const agregarSeccion = () => {
    setSecciones(prev => [...prev, { nombre: '', tipo: 'GENERAL', capacidad: 500, precio: 100 }])
  }

  const actualizarSeccion = (index, field, value) => {
    setSecciones(prev => prev.map((s, i) => i === index ? { ...s, [field]: value } : s))
  }

  const eliminarSeccion = (index) => {
    setSecciones(prev => prev.filter((_, i) => i !== index))
  }

  const handleCrearEvento = async (e) => {
    e.preventDefault()
    if (!eventoForm.lugarId) { toast.error('Selecciona un lugar'); return }
    if (secciones.length === 0) { toast.error('Agrega al menos una sección'); return }
    setCreandoEvento(true)
    try {
      const evRes = await api.post('/eventos', eventoForm)
      const eventoId = evRes.data.datos.id
      await api.put(`/eventos/${eventoId}/publicar`, {
        secciones: secciones.map(s => ({
          nombre: s.nombre,
          tipo: s.tipo,
          capacidad: Number(s.capacidad),
          precio: Number(s.precio)
        }))
      })
      setEventos(prev => [...prev, { ...evRes.data.datos, estado: 'ACTIVO' }])
      toast.success('Evento creado y publicado exitosamente')
      setEventoForm({ nombre: '', descripcion: '', lugarId: '', fechaEvento: '', tipoEvento: 'MIXTO' })
      setSecciones([{ nombre: 'General', tipo: 'GENERAL', capacidad: 1000, precio: 150 }])
      setTab('eventos')
    } catch (err) {
      toast.error(err.response?.data?.mensaje || 'Error al crear evento')
    } finally {
      setCreandoEvento(false)
    }
  }

  const formatFecha = (fecha) => new Date(fecha).toLocaleDateString('es-GT', {
    year: 'numeric', month: 'short', day: 'numeric'
  })

  const tabs = [
    { id: 'eventos', label: 'Eventos', icon: <Music size={16} /> },
    { id: 'crear-lugar', label: 'Crear lugar', icon: <MapPin size={16} /> },
    { id: 'crear-evento', label: 'Crear evento', icon: <Plus size={16} /> },
  ]

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-6xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-white mb-8 flex items-center gap-3">
          <Settings className="text-purple-400" size={32} />
          Panel de administración
        </h1>

        <div className="flex gap-2 mb-8 bg-gray-900 p-1 rounded-xl w-fit">
          {tabs.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
                tab === t.id ? 'bg-purple-600 text-white' : 'text-gray-400 hover:text-white'
              }`}>
              {t.icon} {t.label}
            </button>
          ))}
        </div>

        {tab === 'eventos' && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
              <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                <p className="text-gray-400 text-sm mb-1">Total eventos</p>
                <p className="text-3xl font-bold text-white">{eventos.length}</p>
              </div>
              <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                <p className="text-gray-400 text-sm mb-1">Eventos activos</p>
                <p className="text-3xl font-bold text-green-400">
                  {eventos.filter(e => e.estado === 'ACTIVO').length}
                </p>
              </div>
              <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                <p className="text-gray-400 text-sm mb-1">Agotados</p>
                <p className="text-3xl font-bold text-red-400">
                  {eventos.filter(e => e.estado === 'AGOTADO').length}
                </p>
              </div>
            </div>

            {loading ? (
              <div className="flex justify-center py-12">
                <Loader2 className="animate-spin text-purple-400" size={40} />
              </div>
            ) : (
              <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-gray-800">
                      <th className="text-left text-gray-400 text-sm font-medium px-6 py-4">Evento</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-6 py-4">Fecha</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-6 py-4">Tipo</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-6 py-4">Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {eventos.map(evento => (
                      <tr key={evento.id} className="border-b border-gray-800 hover:bg-gray-800 transition">
                        <td className="px-6 py-4">
                          <p className="text-white font-medium">{evento.nombre}</p>
                          <p className="text-gray-500 text-xs">{evento.descripcion}</p>
                        </td>
                        <td className="px-6 py-4 text-gray-300 text-sm">{formatFecha(evento.fechaEvento)}</td>
                        <td className="px-6 py-4 text-gray-400 text-sm">{evento.tipoEvento}</td>
                        <td className="px-6 py-4">
                          <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                            evento.estado === 'ACTIVO' ? 'bg-green-900 text-green-300' :
                            evento.estado === 'AGOTADO' ? 'bg-red-900 text-red-300' :
                            'bg-gray-700 text-gray-300'
                          }`}>{evento.estado}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {tab === 'crear-lugar' && (
          <div className="max-w-lg">
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8">
              <h2 className="text-xl font-bold text-white mb-6">Crear nuevo lugar</h2>
              <form onSubmit={handleCrearLugar} className="space-y-4">
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Nombre del lugar</label>
                  <input type="text" required
                    value={lugarForm.nombre}
                    onChange={e => setLugarForm({...lugarForm, nombre: e.target.value})}
                    placeholder="Ej: Estadio Nacional"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition" />
                </div>
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Dirección</label>
                  <input type="text"
                    value={lugarForm.direccion}
                    onChange={e => setLugarForm({...lugarForm, direccion: e.target.value})}
                    placeholder="Ej: Zona 5"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition" />
                </div>
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Ciudad</label>
                  <input type="text"
                    value={lugarForm.ciudad}
                    onChange={e => setLugarForm({...lugarForm, ciudad: e.target.value})}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition" />
                </div>
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Capacidad máxima</label>
                  <input type="number" required min="1"
                    value={lugarForm.capacidadMaxima}
                    onChange={e => setLugarForm({...lugarForm, capacidadMaxima: Number(e.target.value)})}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-purple-500 transition" />
                </div>
                <button type="submit" disabled={creandoLugar}
                  className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-gray-700 text-white font-semibold py-3 rounded-xl transition flex items-center justify-center gap-2">
                  {creandoLugar ? <><Loader2 className="animate-spin" size={18} /> Creando...</> : <><Plus size={18} /> Crear lugar</>}
                </button>
              </form>

              {lugares.length > 0 && (
                <div className="mt-6 pt-6 border-t border-gray-800">
                  <p className="text-gray-400 text-sm mb-3">Lugares creados ({lugares.length})</p>
                  <div className="space-y-2">
                    {lugares.map(l => (
                      <div key={l.id} className="flex items-center gap-2 text-sm text-gray-300">
                        <CheckCircle size={14} className="text-green-400" />
                        {l.nombre} — {l.ciudad}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {tab === 'crear-evento' && (
          <div className="max-w-2xl">
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8">
              <h2 className="text-xl font-bold text-white mb-6">Crear y publicar evento</h2>
              <form onSubmit={handleCrearEvento} className="space-y-4">
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Nombre del evento</label>
                  <input type="text" required
                    value={eventoForm.nombre}
                    onChange={e => setEventoForm({...eventoForm, nombre: e.target.value})}
                    placeholder="Ej: Bad Bunny Guatemala 2026"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition" />
                </div>
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Descripción</label>
                  <textarea rows={3}
                    value={eventoForm.descripcion}
                    onChange={e => setEventoForm({...eventoForm, descripcion: e.target.value})}
                    placeholder="Descripción del evento"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition resize-none" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm text-gray-300 mb-1">Lugar</label>
                    <select required
                      value={eventoForm.lugarId}
                      onChange={e => setEventoForm({...eventoForm, lugarId: e.target.value})}
                      className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-purple-500 transition">
                      <option value="">Seleccionar lugar</option>
                      {lugares.map(l => (
                        <option key={l.id} value={l.id}>{l.nombre}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm text-gray-300 mb-1">Tipo</label>
                    <select
                      value={eventoForm.tipoEvento}
                      onChange={e => setEventoForm({...eventoForm, tipoEvento: e.target.value})}
                      className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-purple-500 transition">
                      <option value="MIXTO">MIXTO</option>
                      <option value="SOLO_GENERAL">SOLO GENERAL</option>
                      <option value="SOLO_NUMERADO">SOLO NUMERADO</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-sm text-gray-300 mb-1">Fecha del evento</label>
                  <input type="datetime-local" required
                    value={eventoForm.fechaEvento}
                    onChange={e => setEventoForm({...eventoForm, fechaEvento: e.target.value})}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-purple-500 transition" />
                </div>

                <div className="pt-4 border-t border-gray-800">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-white font-semibold">Secciones</h3>
                    <button type="button" onClick={agregarSeccion}
                      className="flex items-center gap-1 text-purple-400 hover:text-purple-300 text-sm transition">
                      <Plus size={16} /> Agregar sección
                    </button>
                  </div>
                  <div className="space-y-4">
                    {secciones.map((sec, i) => (
                      <div key={i} className="bg-gray-800 rounded-xl p-4">
                        <div className="grid grid-cols-2 gap-3 mb-3">
                          <div>
                            <label className="block text-xs text-gray-400 mb-1">Nombre</label>
                            <input type="text" required
                              value={sec.nombre}
                              onChange={e => actualizarSeccion(i, 'nombre', e.target.value)}
                              placeholder="VIP, General..."
                              className="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-purple-500 transition" />
                          </div>
                          <div>
                            <label className="block text-xs text-gray-400 mb-1">Tipo</label>
                            <select value={sec.tipo}
                              onChange={e => actualizarSeccion(i, 'tipo', e.target.value)}
                              className="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-purple-500 transition">
                              <option value="GENERAL">GENERAL</option>
                              <option value="NUMERADO">NUMERADO</option>
                            </select>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="block text-xs text-gray-400 mb-1">Capacidad</label>
                            <input type="number" required min="1"
                              value={sec.capacidad}
                              onChange={e => actualizarSeccion(i, 'capacidad', e.target.value)}
                              className="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-purple-500 transition" />
                          </div>
                          <div>
                            <label className="block text-xs text-gray-400 mb-1">Precio (Q)</label>
                            <input type="number" required min="0"
                              value={sec.precio}
                              onChange={e => actualizarSeccion(i, 'precio', e.target.value)}
                              className="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-purple-500 transition" />
                          </div>
                        </div>
                        {secciones.length > 1 && (
                          <button type="button" onClick={() => eliminarSeccion(i)}
                            className="text-red-400 hover:text-red-300 text-xs mt-2 transition">
                            Eliminar sección
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                <button type="submit" disabled={creandoEvento}
                  className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-gray-700 text-white font-semibold py-3 rounded-xl transition flex items-center justify-center gap-2 mt-4">
                  {creandoEvento
                    ? <><Loader2 className="animate-spin" size={18} /> Creando y publicando...</>
                    : <><Music size={18} /> Crear y publicar evento</>}
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
