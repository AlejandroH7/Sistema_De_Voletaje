# Fase 2 Front Alejo — Login y Registro

## Fecha
21 de mayo de 2026

## Rama
dev

## Objetivo
Se trabajó la fase 2 del frontend, enfocada en mejorar las páginas de Login y Registro para el sistema Sold-Out Challenge Live con una interfaz profesional, formularios funcionales y conexión al backend mediante Axios.

## Archivos modificados
- frontend/src/pages/LoginPage.jsx
- frontend/src/pages/RegisterPage.jsx
- frontend/src/context/AuthContext.jsx
- frontend/src/components/ProtectedRoute.jsx

## Archivos creados
- frontend/src/context/AuthContextBase.js
- frontend/src/context/useAuth.js

## Cambios realizados
- Diseño profesional con Tailwind para Login y Registro.
- Uso de React hooks con `useState` para formularios y estados de carga.
- Uso de React Router para navegación entre `/login`, `/registro`, `/`, `/admin` y flujos protegidos.
- Uso de AuthContext para guardar sesión al iniciar sesión.
- Separación del hook `useAuth` para cumplir con Fast Refresh y ESLint.
- Uso de Axios para consumir `/api/auth/login` y `/api/auth/registro`.
- Manejo de loading en botones con `Loader2`.
- Manejo de errores con `react-hot-toast`.
- Redirección a `/admin` si el usuario tiene rol `ADMIN`.
- Redirección a `/` si el usuario es cliente.

## Endpoints usados
- POST /api/auth/login
- POST /api/auth/registro

## Pruebas ejecutadas

### npm install
Primer intento falló por permisos al escribir logs en `~/.npm/_logs`. Se repitió con permisos elevados.

Resultado final:
```text
changed 184 packages, and audited 185 packages in 4s
found 0 vulnerabilities
```

### npm run lint
Primer intento falló por reglas existentes en `AuthContext.jsx`:
- `react-hooks/set-state-in-effect`
- `react-refresh/only-export-components`

Se corrigió separando el contexto y hook en archivos dedicados.

Resultado final:
```text
> frontend@0.0.0 lint
> eslint .
```

### npm run build
Primer intento falló porque Vite resolvía de forma ambigua `AuthContext.js` y `AuthContext.jsx` en macOS. Se corrigió usando `AuthContextBase.js`.

Resultado final:
```text
vite v8.0.14 building client environment for production...
✓ 1813 modules transformed.
dist/index.html                   0.45 kB │ gzip:  0.29 kB
dist/assets/index-CQ7gLUem.css   16.24 kB │ gzip:  3.81 kB
dist/assets/index-Cs0DJasm.js   296.99 kB │ gzip: 97.21 kB
✓ built in 152ms
```

### npm run dev
Primer intento falló por permisos del sandbox al abrir `::1:5173`. Se repitió con permiso elevado.

Resultado final:
```text
VITE v8.0.14 ready in 231 ms
Local: http://localhost:5173/
```

El servidor fue detenido manualmente con `Ctrl+C` después de verificar la URL local.

## Resultado
Las pruebas obligatorias pasaron después de corregir problemas de lint/build existentes en la estructura de AuthContext y de ejecutar comandos que requerían permisos elevados para npm/Vite.

## Estado Git
```text
## dev...origin/dev
 M frontend/src/components/ProtectedRoute.jsx
 M frontend/src/context/AuthContext.jsx
 M frontend/src/pages/LoginPage.jsx
 M frontend/src/pages/RegisterPage.jsx
?? documentos/
?? frontend/src/context/AuthContextBase.js
?? frontend/src/context/useAuth.js
```

## Pendientes
- Probar manualmente el flujo real contra backend activo.
- Validar estilos visuales en navegador con usuarios reales o datos de prueba.
- Continuar con las páginas de eventos, reservas y pagos en la siguiente fase del frontend.

## Confirmación
No se hizo commit ni push durante esta tarea.
