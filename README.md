# 🎟️ Sold-Out Challenge Live

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Docker](https://img.shields.io/badge/Docker%20Compose-ready-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15--alpine-336791)

Plataforma de boletaje para eventos masivos diseñada para soportar alta concurrencia, reservas temporales, pagos idempotentes, mensajería asíncrona y observabilidad operativa.

## 📚 Tabla de contenidos

1. [Problema de negocio](#-problema-de-negocio)
2. [Arquitectura general](#-arquitectura-general)
3. [Microservicios](#-microservicios)
4. [Tecnologías](#-tecnologías)
5. [Prerrequisitos](#-prerrequisitos)
6. [Levantar infraestructura](#-levantar-infraestructura)
7. [Levantar sistema completo](#-levantar-sistema-completo)
8. [Health checks](#-health-checks)
9. [Estructura del repositorio](#-estructura-del-repositorio)
10. [Flujo de compra](#-flujo-de-compra)
11. [Variables de entorno](#-variables-de-entorno)
12. [Git workflow](#-git-workflow)
13. [Equipo](#-equipo)
14. [Kafka topics](#-kafka-topics)
15. [Pruebas](#-pruebas)
16. [Observabilidad](#-observabilidad)
17. [Troubleshooting](#-troubleshooting)

## 🧩 Problema de negocio

En eventos masivos, miles de usuarios intentan comprar entradas al mismo tiempo. El sistema debe evitar sobreventa, garantizar que una reserva temporal no bloquee inventario para siempre, impedir pagos duplicados por reintentos, notificar de forma idempotente y mantener trazabilidad cuando un servicio falle.

Sold-Out Challenge Live resuelve este problema con microservicios especializados, una base por dominio, Redis para TTL, Kafka para coordinación asíncrona, API Gateway para seguridad centralizada y Prometheus/Grafana para monitoreo.

## 🏗️ Arquitectura general

```text
                              ┌────────────────────┐
                              │ Cliente Web/Móvil  │
                              └─────────┬──────────┘
                                        │ HTTP + JWT
                              ┌─────────▼──────────┐
                              │ api-gateway :8080  │
                              │ valida JWT         │
                              │ inyecta headers    │
                              └─────────┬──────────┘
          ┌─────────────────────────────┼─────────────────────────────┐
          │                             │                             │
┌─────────▼────────┐          ┌─────────▼────────┐          ┌─────────▼────────┐
│ ms-usuarios:8081 │          │ ms-eventos:8082  │          │ ms-inventario:8083│
│ auth/JWT         │          │ catálogo         │          │ disponibilidad    │
│ db_usuarios      │          │ db_eventos       │          │ db_inventario     │
└──────────────────┘          └─────────┬────────┘          └─────────┬────────┘
                                        │ Kafka evento.creado          │ Redis inventario
                              ┌─────────▼────────┐          ┌─────────▼────────┐
                              │ Kafka            │◄────────►│ Redis            │
                              │ eventos dominio  │          │ TTL/cache        │
                              └─────────┬────────┘          └──────────────────┘
          ┌─────────────────────────────┼─────────────────────────────┐
┌─────────▼────────┐          ┌─────────▼────────┐          ┌─────────▼────────┐
│ ms-reservas:8084 │          │ ms-pagos:8085    │          │ ms-notif:8086    │
│ Saga reservas    │          │ idempotencia     │          │ notificaciones   │
│ db_reservas      │          │ outbox/db_pagos  │          │ db_notificaciones│
└──────────────────┘          └──────────────────┘          └──────────────────┘

                 ┌────────────────────┐        ┌────────────────────┐
                 │ Prometheus :9090   │◄──────►│ Grafana :3000      │
                 │ métricas           │        │ dashboards         │
                 └────────────────────┘        └────────────────────┘
```

## 🧱 Microservicios

| Servicio | Puerto | Responsabilidad | Estado |
|---|---:|---|---|
| api-gateway | 8080 | Entrada única, JWT, autorización por rol, headers internos | Implementado |
| ms-usuarios | 8081 | Registro, login, BCrypt, emisión JWT | Implementado |
| ms-eventos | 8082 | Lugares, eventos, publicación de evento.creado | Pendiente |
| ms-inventario | 8083 | Secciones, asientos, anti-sobreventa, Redis | Pendiente |
| ms-reservas | 8084 | Reservas, TTL, Saga, idempotencia | Pendiente |
| ms-pagos | 8085 | Pagos idempotentes, Outbox Pattern | Pendiente |
| ms-notificaciones | 8086 | Emails/SMS/PUSH simulados, deduplicación | Pendiente |

## 🛠️ Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje base |
| Spring Boot | 3.x | Framework de microservicios |
| Spring Cloud Gateway | 2022.0.4 | Gateway reactivo |
| PostgreSQL | 15-alpine | Persistencia ACID |
| Redis | 7-alpine | TTL de reservas y cache |
| Kafka | Confluent 7.5.0 | Eventos asíncronos |
| Docker Compose | v2 | Orquestación local |
| Prometheus | latest | Métricas |
| Grafana | latest | Dashboards |
| JJWT | 0.11.5 | JWT HS256 |

## ✅ Prerrequisitos

- Docker Desktop activo.
- Java 17 instalado.
- Maven 3.9+ instalado.
- Puertos libres: 8080-8086, 5432 o 5433, 6379, 9092, 9090, 3000.
- Archivo `.env` creado desde `.env.ejemplo`.

💡 En Mac Apple Silicon se usan imágenes `maven:3.9-eclipse-temurin-17` y `eclipse-temurin:17-jre-jammy`.

## 🚀 Levantar infraestructura

```bash
cp .env.ejemplo .env
# editar .env si hace falta
docker-compose up -d postgres redis zookeeper kafka prometheus grafana
docker-compose ps
```

Verificar PostgreSQL:

```bash
docker exec soldout-postgres psql -U admin -d postgres -c "\l"
docker exec soldout-postgres psql -U admin -d db_usuarios -c "\dt"
```

## 🧪 Levantar sistema completo

```bash
docker-compose up -d --build
docker-compose ps
```

Para levantar solo un servicio:

```bash
docker-compose up -d --build ms-usuarios
docker-compose up -d --build api-gateway
```

## 🩺 Health checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
```

Login de prueba:

```bash
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alejandro@soldout.com","contrasena":"password123"}' | python3 -m json.tool
```

## 📁 Estructura del repositorio

```text
api-gateway/                 Gateway reactivo, rutas, JWT, headers internos
ms-usuarios/                 Registro, login, emisión JWT
ms-eventos/                  Catálogo de eventos y lugares
ms-inventario/               Inventario, asientos, anti-sobreventa
ms-reservas/                 Reservas, TTL, Saga
ms-pagos/                    Pagos idempotentes y Outbox
ms-notificaciones/           Notificaciones idempotentes
database/init.sql            Inicialización de las 6 bases de datos
monitoring/prometheus.yml    Scrape config
docs/guias/                  Guías de implementación por servicio
docs/contratos/              Contratos API y Kafka
tests/                       Pruebas futuras e insumos de QA
```

## 🎫 Flujo de compra

1. El usuario inicia sesión en `POST /api/auth/login`.
2. `ms-usuarios` devuelve JWT con `usuario_id`, `email`, `nombre`, `rol`.
3. El cliente consulta eventos públicos con `GET /api/eventos`.
4. El cliente consulta disponibilidad con `GET /api/inventario/{eventoId}/secciones`.
5. El usuario crea reserva con `POST /api/reservas` y header `Idempotency-Key`.
6. `ms-reservas` llama a `ms-inventario` para reservar cupo/asientos.
7. `ms-reservas` guarda reserva `PENDIENTE`, TTL de 600s en Redis y publica `reserva.creada`.
8. `ms-pagos` consume o recibe solicitud de pago, procesa idempotentemente y escribe en `pagos_salida`.
9. Scheduler Outbox publica `pago.confirmado` o `pago.fallido`.
10. `ms-reservas`, `ms-inventario` y `ms-notificaciones` reaccionan al evento.
11. Si el pago confirma, la reserva queda `CONFIRMADO` y los asientos `VENDIDO`.
12. Si falla o expira, se libera inventario.

## 🔐 Variables de entorno

| Variable | Propósito |
|---|---|
| POSTGRES_HOST, POSTGRES_PORT | Host/puerto PostgreSQL |
| POSTGRES_USER, POSTGRES_PASSWORD | Credenciales |
| DB_EVENTOS, DB_INVENTARIO, DB_RESERVAS, DB_PAGOS, DB_NOTIFICACIONES | Bases por dominio |
| REDIS_HOST, REDIS_PORT | Redis |
| KAFKA_BOOTSTRAP_SERVERS | Broker interno, `kafka:29092` |
| JWT_SECRET, JWT_EXPIRACION_MS | Seguridad JWT |
| RESERVA_TTL_SEGUNDOS | TTL de reservas, default 600 |

⚠️ Nunca subir `.env`; ya está protegido por `.gitignore`.

## 🌿 Estrategia Git

- `main`: versión estable.
- `develop`: integración diaria.
- `feature/<servicio>-<descripcion>`: trabajo por microservicio.
- `fix/<servicio>-<bug>`: correcciones.
- Pull Request obligatorio para integrar.
- Cada PR debe incluir evidencia: build, health check, pruebas/curl.

## 👥 Equipo

| Persona | Rol | Servicios |
|---|---|---|
| Alejandro | Líder técnico | api-gateway, ms-usuarios, infraestructura |
| Eric | Backend | ms-inventario, ms-notificaciones |
| Josue | Backend | ms-eventos, ms-reservas |
| Daniel | Backend | ms-pagos |

## 📨 Kafka topics

| Topic | Producer | Consumers |
|---|---|---|
| evento.creado | ms-eventos | ms-inventario |
| reserva.creada | ms-reservas | ms-pagos |
| pago.confirmado | ms-pagos | ms-reservas, ms-inventario, ms-notificaciones |
| pago.fallido | ms-pagos | ms-reservas, ms-inventario, ms-notificaciones |
| reserva.expirada | ms-reservas | ms-inventario, ms-notificaciones |
| reserva.confirmada | ms-reservas | ms-notificaciones |

Política: 3 reintentos, 1s backoff y luego Dead Letter Topic.

## 🧪 Pruebas

```bash
cd ms-usuarios && mvn test
cd api-gateway && mvn test
docker-compose up -d --build
curl http://localhost:8080/actuator/health
```

Para pruebas manuales usar los contratos en [docs/contratos/API_CONTRATOS.md](docs/contratos/API_CONTRATOS.md).

## 📊 Observabilidad

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Credenciales Grafana: `admin / admin`

Prometheus scrapea `/actuator/prometheus` cuando el servicio expone `micrometer-registry-prometheus`.

## 🧯 Troubleshooting

| Problema | Causa probable | Solución |
|---|---|---|
| `port 5432 already in use` | PostgreSQL local activo | Cambiar `POSTGRES_PORT=5433` o detener proceso local |
| Gateway devuelve 500 a `ms-eventos` | Servicio destino no existe/no está en red | Levantar servicio o revisar `docker network inspect` |
| Login devuelve `TOKEN_INVALIDO` | Secret JWT mal tratado | Usar bytes UTF-8 en `JwtUtil` |
| `relation does not exist` | `init.sql` no corrió | Borrar volumen solo si es entorno local y recrear |
| Kafka no resuelve host | Listeners incorrectos | Usar `kafka:29092` dentro de Docker |
| Maven falla en Docker | Imagen incompatible | Usar Dockerfile estándar del proyecto |

💡 Para ver logs:

```bash
docker logs soldout-ms-usuarios -f
docker logs soldout-api-gateway -f
```
