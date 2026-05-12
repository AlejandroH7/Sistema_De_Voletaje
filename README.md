# 🎟️ Sold-Out Challenge Live

Plataforma de boletaje para eventos masivos con alta concurrencia.

## Microservicios

| Servicio | Puerto | Descripción |
|---|---|---|
| api-gateway | 8080 | Punto de entrada único, JWT, rate limiting |
| ms-usuarios | 8081 | Registro, login y autenticación |
| ms-eventos | 8082 | Catálogo de eventos y venues |
| ms-inventario | 8083 | Disponibilidad y reserva de asientos |
| ms-reservas | 8084 | Gestión de reservas y saga de compra |
| ms-pagos | 8085 | Procesamiento de pagos con idempotencia |
| ms-notificaciones | 8086 | Notificaciones vía Kafka |

## Tecnologías

- Java 17 + Spring Boot 3.x
- PostgreSQL (6 bases de datos separadas)
- Redis (caché y TTL de reservas)
- Apache Kafka (mensajería asíncrona)
- Docker Compose
- Prometheus + Grafana

## Levantar infraestructura (Día 1)

```bash
cp .env.ejemplo .env
# Edita .env con tus valores
docker-compose up -d postgres redis zookeeper kafka prometheus grafana
```

## Levantar sistema completo

```bash
docker-compose up -d --build
```

## Equipo

- Alejandro (Líder técnico) — api-gateway, ms-usuarios, infraestructura
- Eric — ms-inventario, ms-notificaciones
- Josue — ms-reservas, ms-eventos
- Daniel — ms-pagos
