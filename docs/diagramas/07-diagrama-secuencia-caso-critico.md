# 7. Diagrama de secuencia del caso critico

```mermaid
sequenceDiagram
    actor Cliente
    participant Gateway as API Gateway
    participant Usuarios as ms-usuarios
    participant Eventos as ms-eventos
    participant Inventario as ms-inventario
    participant Reservas as ms-reservas
    participant Pagos as ms-pagos
    participant Notificaciones as ms-notificaciones
    participant PostgreSQL
    participant Redis
    participant Kafka

    opt Inicio de sesion si el cliente aun no tiene JWT
        Cliente->>Gateway: POST /api/auth/login
        Gateway->>Usuarios: Enruta login
        Usuarios->>PostgreSQL: Valida usuario
        Usuarios-->>Gateway: JWT
        Gateway-->>Cliente: JWT
    end

    Cliente->>Gateway: GET /api/eventos
    Gateway->>Eventos: Consulta eventos
    Eventos->>PostgreSQL: Lee eventos activos
    Eventos-->>Gateway: Eventos
    Gateway-->>Cliente: Eventos disponibles

    Cliente->>Gateway: GET /api/inventario/{eventoId}/secciones
    Gateway->>Inventario: Consulta disponibilidad
    Inventario->>Redis: Lee cache de disponibilidad
    alt Cache miss
        Inventario->>PostgreSQL: Lee inventario
        Inventario->>Redis: Guarda disponibilidad
    end
    Inventario-->>Gateway: Disponibilidad
    Gateway-->>Cliente: Secciones disponibles

    Cliente->>Gateway: POST /api/reservas + Idempotency-Key
    Gateway->>Reservas: Crear reserva
    Reservas->>Inventario: Reservar asientos de forma atomica
    Inventario->>PostgreSQL: UPDATE inventario WHERE disponibles >= cantidad
    Inventario->>Redis: Invalida / actualiza disponibilidad
    Inventario-->>Reservas: Reserva de inventario exitosa
    Reservas->>PostgreSQL: Guarda reserva PENDIENTE
    Reservas->>Redis: Guarda TTL reserva:ttl:{reservaId}
    Reservas->>Kafka: Publica reserva.creada

    Cliente->>Gateway: POST /api/pagos + Idempotency-Key
    Gateway->>Pagos: Procesar pago
    Pagos->>PostgreSQL: Verifica idempotencia y guarda pago
    Pagos->>PostgreSQL: Guarda pagos_salida

    alt Pago confirmado
        Pagos->>Kafka: Outbox publica pago.confirmado
        Kafka-->>Reservas: pago.confirmado
        Reservas->>PostgreSQL: PENDIENTE -> CONFIRMADO
        Kafka-->>Inventario: pago.confirmado
        Inventario->>PostgreSQL: Reservados -> vendidos
        Kafka-->>Notificaciones: pago.confirmado
        Notificaciones->>PostgreSQL: Registra RESERVA_CONFIRMADA
        Pagos-->>Gateway: Pago completado
        Gateway-->>Cliente: Respuesta final exitosa
    else Pago fallido
        Pagos->>Kafka: Outbox publica pago.fallido
        Kafka-->>Reservas: pago.fallido
        Reservas->>PostgreSQL: Marca reserva CANCELADO
        Kafka-->>Inventario: pago.fallido
        Inventario->>PostgreSQL: Libera inventario
        Kafka-->>Notificaciones: pago.fallido
        Notificaciones->>PostgreSQL: Registra PAGO_FALLIDO
        Pagos-->>Gateway: Pago fallido
        Gateway-->>Cliente: Respuesta de fallo
    end

    alt Reserva expira antes de confirmarse
        Reservas->>PostgreSQL: Scheduler detecta expira_en < NOW()
        Reservas->>Kafka: Publica reserva.expirada
        Kafka-->>Inventario: reserva.expirada
        Inventario->>PostgreSQL: Libera inventario
        Kafka-->>Notificaciones: reserva.expirada
        Notificaciones->>PostgreSQL: Registra RESERVA_EXPIRADA
    end
```

La compra de boleto es el caso critico porque combina consistencia de inventario, idempotencia de pago, expiracion temporal y coordinacion asincrona. El flujo principal confirma la reserva y vende los asientos; las ramas alternativas compensan un fallo de pago o una expiracion sin dejar inventario bloqueado.
