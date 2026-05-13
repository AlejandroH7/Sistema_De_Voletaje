# 💳 Guía Día 1 — ms-pagos

Responsable: Daniel  
Puerto: `8085`  
Base: `db_pagos`  
Consumer group: `grupo-pagos`

## 1. 🎯 Propósito

`ms-pagos` es crítico porque maneja dinero. Cobrar dos veces es un error grave; por eso usa idempotencia. También implementa Outbox Pattern para no perder eventos: el pago y el evento saliente se guardan en la misma transacción.

## 2. ✅ Tareas Día 1

Dependencias:

- Spring Web
- Spring Data JPA
- Actuator
- Validation
- Lombok
- PostgreSQL driver
- Spring Kafka
- Micrometer Prometheus

## 3. 📁 Estructura

```text
ms-pagos/src/main/java/com/soldout/pagos/
├── MsPagosApplication.java
├── controller/PagoController.java
├── dto/CrearPagoRequest.java
├── dto/PagoResponse.java
├── dto/RespuestaApi.java
├── entity/Pago.java
├── entity/PagosSalida.java
├── repository/PagoRepository.java
├── repository/PagosSalidaRepository.java
├── service/PagoService.java
├── service/outbox/OutboxPublisherScheduler.java
└── kafka/PagoKafkaProducer.java
```

## 4. ⚙️ application.yml

```yaml
server:
  port: 8085
spring:
  application:
    name: ms-pagos
  datasource:
    url: jdbc:postgresql://localhost:5432/db_pagos
    username: admin
    password: admin123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: grupo-pagos
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

## 5. 🐳 application-docker.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DB_PAGOS}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

## 6. 🐳 Dockerfile

Usar estándar con `EXPOSE 8085`.

## 7. 🧱 Entidades

### Pago

```java
@Entity
@Table(name = "pagos")
public class Pago {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(name="reserva_id", nullable=false, unique=true) private UUID reservaId;
  @Column(name="usuario_id", nullable=false) private UUID usuarioId;
  @Column(nullable=false, precision=10, scale=2) private BigDecimal monto;
  @Column(nullable=false, length=3) private String moneda = "GTQ";
  @Column(nullable=false, length=20) private String estado = "PENDIENTE";
  @Column(name="clave_idempotencia", nullable=false, unique=true, length=64) private String claveIdempotencia;
  @Column(name="metodo_pago", nullable=false, length=50) private String metodoPago = "TARJETA";
  @Column(name="motivo_fallo") private String motivoFallo;
  @Column(name="procesado_en") private LocalDateTime procesadoEn;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
}
```

`reserva_id UNIQUE` garantiza un pago por reserva. `clave_idempotencia UNIQUE` evita cobros duplicados. Estados: `PENDIENTE -> COMPLETADO | FALLIDO -> REEMBOLSADO`.

### PagosSalida

```java
@Entity
@Table(name = "pagos_salida")
public class PagosSalida {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(name="id_agregado", nullable=false) private UUID idAgregado;
  @Column(name="tipo_evento", nullable=false, length=100) private String tipoEvento;
  @Column(nullable=false, columnDefinition="jsonb") private String payload;
  @Column(nullable=false) private Boolean publicado = false;
  @Column(name="publicado_en") private LocalDateTime publicadoEn;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
}
```

`payload` es JSONB en DB y String en Java para simplificar. Ciclo: `publicado=false` al crear, `true` cuando Kafka confirma envío.

## 8. 🗃️ Repositorios

```java
public interface PagoRepository extends JpaRepository<Pago, UUID> {
  Optional<Pago> findByClaveIdempotencia(String claveIdempotencia);
  boolean existsByClaveIdempotencia(String claveIdempotencia);
  Optional<Pago> findByReservaId(UUID reservaId);
}
public interface PagosSalidaRepository extends JpaRepository<PagosSalida, UUID> {
  List<PagosSalida> findByPublicadoFalseOrderByCreadoEnAsc();
}
```

## 9. 🔁 Idempotencia detallada

Pseudocódigo:

```text
recibir POST /api/pagos con Idempotency-Key
buscar pago por clave
si existe COMPLETADO -> retornar 200
si existe PENDIENTE -> retornar 202
si no existe -> insertar PENDIENTE
simular cobro
actualizar COMPLETADO o FALLIDO
insertar pagos_salida publicado=false
retornar resultado
```

Código base:

```java
@Transactional
public PagoResponse procesar(CrearPagoRequest req, String clave) {
  Optional<Pago> existente = pagoRepository.findByClaveIdempotencia(clave);
  if (existente.isPresent()) return mapper.toResponse(existente.get());
  Pago pago = new Pago();
  pago.setEstado("PENDIENTE");
  pago.setClaveIdempotencia(clave);
  pagoRepository.save(pago);
  boolean exito = random.nextInt(100) < 70;
  pago.setEstado(exito ? "COMPLETADO" : "FALLIDO");
  pago.setProcesadoEn(LocalDateTime.now());
  outboxRepository.save(crearEventoSalida(pago));
  return mapper.toResponse(pago);
}
```

## 10. 📤 Outbox Pattern

```text
Pago procesado
     ↓
INSERT pagos_salida (publicado=FALSE)
     ↓
[Scheduler cada 5 seg]
     ↓
SELECT * WHERE publicado=FALSE
     ↓
Publicar Kafka
     ↓
UPDATE publicado=TRUE
```

Scheduler:

```java
@Scheduled(fixedDelay = 5000)
public void publicarPendientes() {
  for (PagosSalida outbox : repository.findByPublicadoFalseOrderByCreadoEnAsc()) {
    kafkaTemplate.send(outbox.getTipoEvento(), outbox.getPayload());
    outbox.setPublicado(true);
    outbox.setPublicadoEn(LocalDateTime.now());
    repository.save(outbox);
  }
}
```

## 11. 🎲 Pago simulado

Usar 70% éxito y 30% fallo permite probar compensación y notificaciones sin pasarela real. En producción se reemplaza por integración PSP.

## 12. ✅ Criterios Día 1

- Health `UP`.
- Entidades/repositories compilan.
- Outbox scheduler creado.
- DB muestra `pagos` y `pagos_salida`.

## 13. 🧪 Probar

```bash
docker-compose up -d postgres kafka
docker-compose up -d --build ms-pagos
curl http://localhost:8085/actuator/health
```

## 14. ⚠️ Errores comunes

- Doble cobro: no procesar sin `Idempotency-Key`.
- Evento perdido: no publicar Kafka fuera del Outbox si el pago se guarda en DB.
- JSONB falla: usar `columnDefinition="jsonb"`.
