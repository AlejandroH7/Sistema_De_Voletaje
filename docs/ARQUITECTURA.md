# 🏗️ Arquitectura Técnica — Sold-Out Challenge Live

## 1. 🧠 Decisiones de arquitectura

### Microservicios

Se eligieron microservicios porque el dominio tiene responsabilidades claramente separadas: autenticación, eventos, inventario, reservas, pagos y notificaciones. Esta separación permite escalar inventario y pagos de forma independiente, aislar fallas, dividir el trabajo por equipo y evolucionar contratos sin bloquear todo el sistema.

⚠️ La desventaja es mayor complejidad operativa: red, tracing, consistencia eventual, Kafka, reintentos e idempotencia. Por eso el proyecto define contratos claros, headers internos, topics estables y respuestas estándar.

### PostgreSQL

PostgreSQL se usa por ACID, MVCC, índices robustos, constraints, transacciones y locks. Es especialmente útil para inventario porque evita inconsistencias con `UPDATE ... WHERE disponibles >= N`. Cada microservicio tiene su propia base lógica para mantener límites de dominio.

### Kafka sobre RabbitMQ

Kafka se eligió por throughput, retención, replay, consumer groups y buen encaje con eventos de dominio. Un pago confirmado puede ser reprocesado por `ms-reservas`, `ms-inventario` y `ms-notificaciones` sin acoplarlos directamente.

### Redis

Redis aporta baja latencia y TTL nativo. Se usa para reservas temporales y cache de disponibilidad. El TTL de reserva evita bloquear asientos indefinidamente cuando el usuario abandona el pago.

### Spring Cloud Gateway

El gateway es reactivo, centraliza JWT, permisos por rol e inyección de headers internos. Los servicios internos no validan JWT; confían en los headers `X-Usuario-Id`, `X-Usuario-Rol`, `X-Usuario-Email`.

## 2. 🧭 Diagrama de arquitectura

```text
┌──────────────┐
│ Cliente      │
└──────┬───────┘
       │ HTTP
┌──────▼────────────────────────────────────────────────────────┐
│ api-gateway:8080                                              │
│ - valida JWT                                                  │
│ - autoriza roles                                              │
│ - inyecta X-Usuario-*                                         │
└──────┬────────────────────────────────────────────────────────┘
       │
       ├──► ms-usuarios:8081 ─────► db_usuarios
       ├──► ms-eventos:8082 ──────► db_eventos ─────► Kafka evento.creado
       ├──► ms-inventario:8083 ───► db_inventario ◄─► Redis
       ├──► ms-reservas:8084 ─────► db_reservas ◄───► Redis TTL
       ├──► ms-pagos:8085 ────────► db_pagos ───────► Kafka Outbox
       └──► ms-notificaciones:8086► db_notificaciones

Kafka topics:
evento.creado, reserva.creada, pago.confirmado, pago.fallido,
reserva.expirada, reserva.confirmada

Observabilidad:
Spring Actuator ─► Prometheus:9090 ─► Grafana:3000
```

## 3. 🎫 Flujo completo de compra de boleto

1. `POST /api/auth/login` devuelve JWT.
2. `GET /api/eventos/{id}` entrega detalle público del evento.
3. `GET /api/inventario/{eventoId}/secciones` entrega disponibilidad desde Redis o DB.
4. `POST /api/reservas` inicia Saga:
   - recibe `Idempotency-Key`;
   - valida usuario desde `X-Usuario-Id`;
   - llama a inventario para reservar asientos/cupo;
   - crea reserva `PENDIENTE`;
   - guarda TTL en Redis: `reserva:ttl:{reservaId}`;
   - publica `reserva.creada`.
5. `ms-pagos` consume `reserva.creada` o recibe `POST /api/pagos`.
6. `ms-pagos` verifica idempotencia, simula cobro, actualiza `pagos` y escribe outbox.
7. Scheduler publica `pago.confirmado` o `pago.fallido`.
8. `ms-reservas` confirma o cancela.
9. `ms-inventario` vende o libera asientos.
10. `ms-notificaciones` envía email/SMS/PUSH simulado.

## 4. ⏳ Flujo de expiración de reserva

1. Reserva queda `PENDIENTE` con `expira_en = now + 600s`.
2. Redis guarda `reserva:ttl:{reservaId}` con TTL 600.
3. Scheduler de `ms-reservas` corre cada 60s.
4. Busca `PENDIENTE` donde `expira_en < NOW()`.
5. Marca `EXPIRADO`.
6. Publica `reserva.expirada`.
7. `ms-inventario` libera cupos/asientos.
8. `ms-notificaciones` registra notificación `RESERVA_EXPIRADA`.

## 5. ❌ Flujo de compensación por pago fallido

```text
ms-pagos detecta fallo
    ↓
publica pago.fallido
    ↓
ms-reservas marca CANCELADO
    ↓
ms-inventario libera inventario
    ↓
ms-notificaciones informa PAGO_FALLIDO
```

La compensación debe ser idempotente: si el mismo evento se procesa dos veces, el estado final debe permanecer correcto.

## 6. 🔒 Estrategia de consistencia

| Riesgo | Técnica |
|---|---|
| Reservas duplicadas | `clave_idempotencia UNIQUE` |
| Pagos duplicados | `clave_idempotencia UNIQUE` y `reserva_id UNIQUE` |
| Notificaciones duplicadas | `clave_idempotencia UNIQUE` |
| Sobreventa | `UPDATE ... WHERE asientos_disponibles >= N` |
| Eventos perdidos de pagos | Outbox Pattern |
| Conflictos de inventario | `version BIGINT` para optimistic locking |

## 7. 🛡️ Alta disponibilidad y fallas

| Falla | Comportamiento esperado |
|---|---|
| Cae ms-inventario | Reservas nuevas fallan controladamente; no se descuenta inventario sin confirmación |
| Cae Redis | Se usa DB como fuente de verdad; TTL puede degradarse a scheduler por `expira_en` |
| Cae Kafka | Outbox conserva eventos no publicados; consumers reintentan |
| Cae ms-pagos | Reservas quedan pendientes hasta expirar; no se confirma sin pago |
| Cae ms-notificaciones | No afecta compra; se reprocesa por Kafka/DLT |

## 8. 🧠 Cache Redis

| Key | Valor | TTL | Propósito |
|---|---|---:|---|
| `inventario:disponible:{seccionId}` | Integer | sin TTL | Disponibilidad rápida |
| `reserva:ttl:{reservaId}` | `"activa"` | 600s | Expiración temporal |
| `evento:detalle:{eventoId}` | JSON | 300s sugerido | Cache opcional de detalle |

## 9. 🗄️ Modelo de datos resumido

| Base | Entidades |
|---|---|
| db_usuarios | usuarios |
| db_eventos | lugares, eventos |
| db_inventario | secciones, inventario_secciones, mesas, asientos |
| db_reservas | reservas, detalle_reserva |
| db_pagos | pagos, pagos_salida |
| db_notificaciones | notificaciones |

## 10. 🧑‍💻 Guía de desarrollo

### Crear un endpoint

1. Crear DTO request/response.
2. Validar con `jakarta.validation`.
3. Agregar método en controller.
4. Mantener formato `RespuestaApi`.
5. Registrar errores con `NegocioException`.
6. Probar con curl y documentar en `API_CONTRATOS.md`.

### Añadir evento Kafka

1. Definir topic y payload.
2. Documentar producer/consumer.
3. Crear DTO de evento.
4. Publicar con `KafkaTemplate`.
5. Consumir con `@KafkaListener`.
6. Configurar reintentos y DLT.

### Convenciones

- Paquetes: `com.soldout.<servicio>`.
- Campos JSON en snake_case para contratos externos.
- Estados en mayúsculas.
- Métodos idempotentes deben aceptar `Idempotency-Key`.

### Debugging

```bash
docker logs soldout-api-gateway -f
docker logs soldout-ms-usuarios -f
docker exec soldout-postgres psql -U admin -d db_reservas -c "select * from reservas;"
```
