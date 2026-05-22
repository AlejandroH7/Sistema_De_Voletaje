import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import EventosPage from './pages/EventosPage'
import EventoDetallePage from './pages/EventoDetallePage'
import ReservaPage from './pages/ReservaPage'
import PagoPage from './pages/PagoPage'
import MisReservasPage from './pages/MisReservasPage'
import AdminPage from './pages/AdminPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/" element={<EventosPage />} />
      <Route path="/eventos/:id" element={<EventoDetallePage />} />
      <Route path="/reservar" element={
        <ProtectedRoute><ReservaPage /></ProtectedRoute>
      } />
      <Route path="/pago/:reservaId" element={
        <ProtectedRoute><PagoPage /></ProtectedRoute>
      } />
      <Route path="/mis-reservas" element={
        <ProtectedRoute><MisReservasPage /></ProtectedRoute>
      } />
      <Route path="/admin" element={
        <ProtectedRoute adminOnly={true}><AdminPage /></ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}
