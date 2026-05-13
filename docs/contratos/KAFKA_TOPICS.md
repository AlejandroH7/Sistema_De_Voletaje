# 📨 Contratos Kafka — Sold-Out Challenge Live

Política global: 3 intentos, backoff 1 segundo, luego Dead Letter Topic (`<topic>.DLT`). Payloads JSON con `evento_id`, timestamp y datos mínimos para idempotencia.

## 🎤 evento.creado

- Producer: `ms-eventos`
- Consumer: `ms-inventario`
- Consumer group: `grupo-inventario`
- DLT: `evento.creado.DLT`
- Cuándo se publica: al ejecutar `PUT /api/eventos/{id}/publicar`.

Payload:

```json
{"evento_id":"uuid","tipo_evento":"MIXTO","publicado_en":"2026-05-12T18:00:00Z","secciones":[{"nombre":"Dance Floor","tipo":"GENERAL","capacidad":30000,"precio":350.00},{"nombre":"VIP","tipo":"NUMERADO","capacidad":50,"precio":2500.00}]}
```

Consumer `ms-inventario`: crea `secciones`, `inventario_secciones` y opcionalmente asientos/mesas. Si falla, reintenta 3 veces; luego DLT para reproceso manual.

## 🧾 reserva.creada

- Producer: `ms-reservas`
- Consumer: `ms-pagos`
- Consumer group: `grupo-pagos`
- DLT: `reserva.creada.DLT`
- Cuándo: después de crear reserva `PENDIENTE`.

Payload:

```json
{"evento_id":"uuid-evento","reserva_id":"uuid-reserva","usuario_id":"uuid-usuario","precio_total":700.00,"moneda":"GTQ","expira_en":"2026-05-12T18:10:00Z","detalles":[{"seccion_id":"uuid","cantidad":2,"precio_unitario":350.00}]}
```

`ms-pagos` puede preparar un pago pendiente o esperar `POST /api/pagos`. Debe usar `reserva_id` como parte de idempotencia.

## ✅ pago.confirmado

- Producer: `ms-pagos`
- Consumers: `ms-reservas`, `ms-inventario`, `ms-notificaciones`
- Groups: `grupo-reservas`, `grupo-inventario`, `grupo-notificaciones`
- DLT: `pago.confirmado.DLT`
- Cuándo: pago pasa a `COMPLETADO` y outbox publica.

Payload:

```json
{"pago_id":"uuid-pago","reserva_id":"uuid-reserva","usuario_id":"uuid-usuario","monto":700.00,"moneda":"GTQ","procesado_en":"2026-05-12T18:02:00Z"}
```

Acciones:

- `ms-reservas`: `PENDIENTE -> CONFIRMADO`.
- `ms-inventario`: reservados -> vendidos.
- `ms-notificaciones`: crear `RESERVA_CONFIRMADA`.

## ❌ pago.fallido

- Producer: `ms-pagos`
- Consumers: `ms-reservas`, `ms-inventario`, `ms-notificaciones`
- DLT: `pago.fallido.DLT`

Payload:

```json
{"pago_id":"uuid-pago","reserva_id":"uuid-reserva","usuario_id":"uuid-usuario","monto":700.00,"motivo_fallo":"Tarjeta rechazada","procesado_en":"2026-05-12T18:02:00Z"}
```

Acciones:

- `ms-reservas`: `PENDIENTE -> CANCELADO`.
- `ms-inventario`: liberar reservados.
- `ms-notificaciones`: crear `PAGO_FALLIDO`.

## ⏳ reserva.expirada

- Producer: `ms-reservas`
- Consumers: `ms-inventario`, `ms-notificaciones`
- DLT: `reserva.expirada.DLT`
- Cuándo: scheduler detecta `expira_en < NOW()`.

Payload:

```json
{"reserva_id":"uuid-reserva","usuario_id":"uuid-usuario","evento_id":"uuid-evento","expirada_en":"2026-05-12T18:10:01Z","detalles":[{"seccion_id":"uuid","cantidad":2}]}
```

Acciones:

- `ms-inventario`: liberar inventario.
- `ms-notificaciones`: crear `RESERVA_EXPIRADA`.

## 🎟️ reserva.confirmada

- Producer: `ms-reservas`
- Consumer: `ms-notificaciones`
- Consumer group: `grupo-notificaciones`
- DLT: `reserva.confirmada.DLT`
- Cuándo: reserva queda `CONFIRMADO`.

Payload:

```json
{"reserva_id":"uuid-reserva","usuario_id":"uuid-usuario","evento_id":"uuid-evento","confirmado_en":"2026-05-12T18:02:05Z","qr_simulado":"QR-uuid-reserva"}
```

`ms-notificaciones` envía confirmación final con QR simulado.

## ⚠️ Reintentos y DLT

Cada consumer debe configurar:

```text
max-attempts: 3
backoff: 1000ms
dlt: <topic>.DLT
```

Si un mensaje cae en DLT:

1. Revisar logs del consumer.
2. Verificar si el error es de datos o infraestructura.
3. Corregir causa.
4. Reprocesar manualmente desde DLT si aplica.

💡 Todo consumer debe ser idempotente. Kafka puede entregar un mensaje más de una vez incluso cuando todo funciona correctamente.
