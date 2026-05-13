# 📜 Contratos API — Sold-Out Challenge Live

Todas las APIs deben responder con el formato estándar:

```json
{"exito":true,"mensaje":"descripción","datos":{},"timestamp":"2026-05-12T18:00:00Z"}
```

```json
{"exito":false,"mensaje":"error","codigo_error":"CODIGO_ERROR","datos":null,"timestamp":"2026-05-12T18:00:00Z"}
```

Autenticación: enviar `Authorization: Bearer <token>` al API Gateway. El gateway inyecta `X-Usuario-Id`, `X-Usuario-Rol`, `X-Usuario-Email`.

## 👤 ms-usuarios

### POST /api/auth/registro

Rol: público. Headers: `Content-Type: application/json`.

Request:

```json
{"nombre":"Alejandro Herrera","email":"alejandro@soldout.com","contrasena":"password123","telefono":"55555555","rol":"CLIENTE"}
```

Response 200:

```json
{"exito":true,"mensaje":"Usuario registrado correctamente","datos":{"id":"uuid","nombre":"Alejandro Herrera","email":"alejandro@soldout.com","telefono":"55555555","rol":"CLIENTE","estado":"ACTIVO"},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 409 `EMAIL_YA_REGISTRADO`, 400 `SOLICITUD_INVALIDA`.

```bash
curl -X POST http://localhost:8080/api/auth/registro -H "Content-Type: application/json" -d '{"nombre":"A","email":"a@b.com","contrasena":"password123"}'
```

### POST /api/auth/login

Rol: público.

Request:

```json
{"email":"alejandro@soldout.com","contrasena":"password123"}
```

Response 200:

```json
{"exito":true,"mensaje":"Login exitoso","datos":{"token":"jwt","tipo":"Bearer","expira_en":"2026-05-14T16:13:27Z","usuario":{"id":"uuid","nombre":"Alejandro Herrera","email":"alejandro@soldout.com","rol":"ADMIN"}},"timestamp":"2026-05-13T16:13:27Z"}
```

Errores: 401 `CREDENCIALES_INVALIDAS`, 403 `USUARIO_BLOQUEADO`.

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"alejandro@soldout.com","contrasena":"password123"}'
```

### GET /api/usuarios/perfil

Rol: cualquier autenticado. Headers: `Authorization`.

Response:

```json
{"exito":true,"mensaje":"Perfil obtenido correctamente","datos":{"id":"uuid","nombre":"Alejandro Herrera","email":"alejandro@soldout.com","rol":"ADMIN","estado":"ACTIVO"},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 401 `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`.

```bash
curl http://localhost:8080/api/usuarios/perfil -H "Authorization: Bearer $TOKEN"
```

## 🎤 ms-eventos

### POST /api/eventos

Rol: `ORGANIZADOR` o `ADMIN`.

Request:

```json
{"nombre":"Festival Sold Out","descripcion":"Evento masivo","lugar_id":"uuid","fecha_evento":"2026-08-01T20:00:00","tipo_evento":"MIXTO","secciones":[{"nombre":"Dance Floor","tipo":"GENERAL","capacidad":30000,"precio":350.00},{"nombre":"VIP","tipo":"NUMERADO","capacidad":50,"precio":2500.00}]}
```

Response:

```json
{"exito":true,"mensaje":"Evento creado","datos":{"id":"uuid","nombre":"Festival Sold Out","estado":"BORRADOR"},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 401 `TOKEN_INVALIDO`, 403 `ROL_NO_AUTORIZADO`, 409 `EVENTO_DUPLICADO`.

```bash
curl -X POST http://localhost:8080/api/eventos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @evento.json
```

### GET /api/eventos

Rol: público.

Response:

```json
{"exito":true,"mensaje":"Eventos activos","datos":[{"id":"uuid","nombre":"Festival","fecha_evento":"2026-08-01T20:00:00","estado":"ACTIVO"}],"timestamp":"2026-05-12T18:00:00Z"}
```

```bash
curl http://localhost:8080/api/eventos
```

### GET /api/eventos/{id}

Rol: público.

Response:

```json
{"exito":true,"mensaje":"Evento encontrado","datos":{"id":"uuid","nombre":"Festival","tipo_evento":"MIXTO","lugar":{"id":"uuid","nombre":"Estadio"}},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 404 `EVENTO_NO_ENCONTRADO`.

### PUT /api/eventos/{id}/publicar

Rol: `ORGANIZADOR` o `ADMIN`.

Response:

```json
{"exito":true,"mensaje":"Evento publicado","datos":{"id":"uuid","estado":"ACTIVO"},"timestamp":"2026-05-12T18:00:00Z"}
```

## 📦 ms-inventario

### GET /api/inventario/{eventoId}/secciones

Rol: público.

Response:

```json
{"exito":true,"mensaje":"Secciones disponibles","datos":[{"seccion_id":"uuid","nombre":"General","tipo":"GENERAL","precio":350.00,"disponibles":1200}],"timestamp":"2026-05-12T18:00:00Z"}
```

### POST /api/inventario/{seccionId}/reservar

Rol: interno `ms-reservas` o usuario autenticado vía gateway.

Request:

```json
{"reserva_id":"uuid","cantidad":2,"asientos":["A1","A2"]}
```

Response:

```json
{"exito":true,"mensaje":"Inventario reservado","datos":{"seccion_id":"uuid","reservados":2},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 409 `INVENTARIO_INSUFICIENTE`, 404 `SECCION_NO_ENCONTRADA`.

### POST /api/inventario/{seccionId}/liberar

Request:

```json
{"reserva_id":"uuid","cantidad":2}
```

Response:

```json
{"exito":true,"mensaje":"Inventario liberado","datos":{"seccion_id":"uuid","liberados":2},"timestamp":"2026-05-12T18:00:00Z"}
```

### GET /api/inventario/{seccionId}/asientos

Response:

```json
{"exito":true,"mensaje":"Asientos","datos":[{"id":"uuid","numero_asiento":"A1","fila":"A","estado":"DISPONIBLE"}],"timestamp":"2026-05-12T18:00:00Z"}
```

## 🧾 ms-reservas

### POST /api/reservas

Rol: `CLIENTE`. Header obligatorio: `Idempotency-Key`.

Request:

```json
{"evento_id":"uuid","detalles":[{"seccion_id":"uuid","tipo_asiento":"GENERAL","cantidad":2,"precio_unitario":350.00}]}
```

Response:

```json
{"exito":true,"mensaje":"Reserva creada","datos":{"id":"uuid","estado":"PENDIENTE","expira_en":"2026-05-12T18:10:00Z","precio_total":700.00},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 409 `RESERVA_DUPLICADA`, `INVENTARIO_INSUFICIENTE`.

### GET /api/reservas/{id}

Rol: cualquier autenticado dueño o admin.

Response incluye reserva y detalles.

### DELETE /api/reservas/{id}

Rol: dueño o admin.

Response:

```json
{"exito":true,"mensaje":"Reserva cancelada","datos":{"id":"uuid","estado":"CANCELADO"},"timestamp":"2026-05-12T18:00:00Z"}
```

### GET /api/reservas/usuario/{usuarioId}

Rol: dueño o admin.

Response: lista de reservas.

## 💳 ms-pagos

### POST /api/pagos

Rol: `CLIENTE`. Header obligatorio: `Idempotency-Key`.

Request:

```json
{"reserva_id":"uuid","monto":700.00,"moneda":"GTQ","metodo_pago":"TARJETA"}
```

Response:

```json
{"exito":true,"mensaje":"Pago completado","datos":{"id":"uuid","reserva_id":"uuid","estado":"COMPLETADO","monto":700.00},"timestamp":"2026-05-12T18:00:00Z"}
```

Errores: 202 pago pendiente, 409 `PAGO_DUPLICADO`, 402 `PAGO_FALLIDO`.

### GET /api/pagos/{id}

Rol: cualquier autenticado relacionado o admin.

### GET /api/pagos/reserva/{reservaId}

Rol: cualquier autenticado relacionado o admin.

## 🔔 ms-notificaciones

### GET /api/notificaciones/usuario/{usuarioId}

Rol: dueño o admin.

Response:

```json
{"exito":true,"mensaje":"Notificaciones","datos":[{"id":"uuid","tipo":"RESERVA_CONFIRMADA","canal":"CORREO","estado":"ENVIADO"}],"timestamp":"2026-05-12T18:00:00Z"}
```

Errores generales: 401 `TOKEN_INVALIDO`, 403 `ROL_NO_AUTORIZADO`, 500 `ERROR_INTERNO`.
