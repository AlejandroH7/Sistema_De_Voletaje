# 🧾 Guía Día 1 — ms-reservas

Responsable: Josue  
Puerto: `8084`  
Base: `db_reservas`  
Consumer group: `grupo-reservas`

## 1. 🎯 Propósito

`ms-reservas` crea y gestiona reservas temporales. Es el orquestador central de la Saga de compra porque coordina inventario, pago, expiración y compensación.

```text
ms-reservas ── reservar asientos ──► ms-inventario
     │
     ├── guarda reserva PENDIENTE + TTL Redis
     │
     └── publica reserva.creada ──► Kafka ──► ms-pagos

ms-pagos ──► pago.confirmado / pago.fallido ──► Kafka
     │
     ├── ms-reservas confirma/cancela
     └── ms-inventario vende/libera
```

## 2. ✅ Tareas Día 1

Dependencias:

- Spring Web
- Spring Data JPA
- Actuator
- Validation
- Lombok
- PostgreSQL driver
- Spring Kafka
- Spring Boot Starter Data Redis
- Micrometer Prometheus

## 3. 📁 Estructura

```text
ms-reservas/src/main/java/com/soldout/reservas/
├── MsReservasApplication.java
├── controller/ReservaController.java
├── dto/CrearReservaRequest.java
├── dto/ReservaResponse.java
├── dto/RespuestaApi.java
├── entity/Reserva.java
├── entity/DetalleReserva.java
├── repository/ReservaRepository.java
├── repository/DetalleReservaRepository.java
├── service/ReservaService.java
├── service/saga/BookingSagaService.java
├── scheduler/ReservaExpiracionScheduler.java
└── kafka/ReservaKafkaProducer.java
└── kafka/ReservaKafkaConsumer.java
```

## 4. ⚙️ application.yml

```yaml
server:
  port: 8084
spring:
  application:
    name: ms-reservas
  datasource:
    url: jdbc:postgresql://localhost:5432/db_reservas
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
      group-id: grupo-reservas
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
  data:
    redis:
      host: localhost
      port: 6379
reserva:
  ttl-segundos: 600
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
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DB_RESERVAS}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
reserva:
  ttl-segundos: ${RESERVA_TTL_SEGUNDOS}
```

## 6. 🐳 Dockerfile

Usar estándar con `EXPOSE 8084`.

## 7. 🧱 Entidades

### Reserva

```java
@Entity
@Table(name = "reservas")
public class Reserva {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(name="usuario_id", nullable=false) private UUID usuarioId;
  @Column(name="evento_id", nullable=false) private UUID eventoId;
  @Column(name="precio_total", nullable=false, precision=10, scale=2) private BigDecimal precioTotal;
  @Column(nullable=false, length=20) private String estado = "PENDIENTE";
  @Column(name="clave_idempotencia", nullable=false, unique=true, length=64) private String claveIdempotencia;
  @Column(name="expira_en", nullable=false) private LocalDateTime expiraEn;
  @Column(name="confirmado_en") private LocalDateTime confirmadoEn;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
  @Column(name="actualizado_en", nullable=false) private LocalDateTime actualizadoEn = LocalDateTime.now();
  @OneToMany(mappedBy="reserva", cascade=CascadeType.ALL, orphanRemoval=true) private List<DetalleReserva> detalles = new ArrayList<>();
}
```

`clave_idempotencia UNIQUE` evita reservas duplicadas por reintentos. `expira_en` es crítico para liberar inventario automáticamente. Transiciones válidas: `PENDIENTE -> CONFIRMADO`, `PENDIENTE -> EXPIRADO`, `PENDIENTE -> CANCELADO`.

### DetalleReserva

```java
@Entity
@Table(name = "detalle_reserva")
public class DetalleReserva {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reserva_id", nullable=false) private Reserva reserva;
  @Column(name="seccion_id", nullable=false) private UUID seccionId;
  @Column(name="tipo_asiento", nullable=false, length=20) private String tipoAsiento;
  @Column(name="asiento_id") private UUID asientoId;
  @Column(name="mesa_id") private UUID mesaId;
  @Column(name="numero_asiento", length=20) private String numeroAsiento;
  @Column(name="nombre_seccion", nullable=false, length=200) private String nombreSeccion;
  @Column(nullable=false) private Integer cantidad = 1;
  @Column(name="precio_unitario", nullable=false, precision=10, scale=2) private BigDecimal precioUnitario;
}
```

Si `tipo_asiento=GENERAL`, `asiento_id` puede ser null y se usa `cantidad`. Si `NUMERADO`, debe haber `asiento_id` o identificador de asiento.

## 8. 🗃️ Repositorios

```java
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
  Optional<Reserva> findByClaveIdempotencia(String claveIdempotencia);
  List<Reserva> findByUsuarioId(UUID usuarioId);
  List<Reserva> findByEstadoAndExpiraEnBefore(String estado, LocalDateTime fecha);
  boolean existsByClaveIdempotencia(String claveIdempotencia);
}
public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, UUID> {
  List<DetalleReserva> findByReservaId(UUID reservaId);
}
```

## 9. 🔁 Idempotencia

1. Recibir request con `Idempotency-Key`.
2. Buscar: `SELECT * FROM reservas WHERE clave_idempotencia = ?`.
3. Si existe, retornar reserva existente.
4. Si no existe, crear nueva reserva.
5. Guardar detalles y publicar evento.

Esto evita duplicados cuando el cliente reintenta por timeout.

## 10. ⏳ TTL con Redis

Al crear:

```text
SET reserva:ttl:{reservaId} "activa" EX 600
```

Scheduler:

```java
@Scheduled(fixedDelay = 60000)
public void expirarReservas() {
  List<Reserva> vencidas = reservaRepository.findByEstadoAndExpiraEnBefore("PENDIENTE", LocalDateTime.now());
  for (Reserva reserva : vencidas) {
    reserva.setEstado("EXPIRADO");
    reservaRepository.save(reserva);
    kafkaProducer.publicarReservaExpirada(reserva);
  }
}
```

## 11. 🧭 BookingSagaService

```java
@Service
public class BookingSagaService {
  public ReservaResponse iniciarSaga(CrearReservaRequest request) {
    // 1. validar headers y usuario
    // 2. verificar idempotencia
    // 3. llamar ms-inventario para reservar
    // 4. guardar reserva PENDIENTE
    // 5. guardar TTL Redis
    // 6. publicar reserva.creada
    return null;
  }
  public void confirmarSaga(UUID reservaId) {
    // pago.confirmado -> CONFIRMADO
  }
  public void compensarSaga(UUID reservaId) {
    // pago.fallido o expiracion -> CANCELADO/EXPIRADO y liberar inventario
  }
}
```

## 12. ✅ Criterios Día 1

- Health `UP` en `8084`.
- Entidades y repositorios compilan.
- Tabla `reservas` y `detalle_reserva` visibles.
- Scheduler creado pero puede estar sin lógica completa.

## 13. 🧪 Probar

```bash
docker-compose up -d postgres redis kafka
docker-compose up -d --build ms-reservas
curl http://localhost:8084/actuator/health
```

## 14. ⚠️ Errores comunes

- Duplicados: no omitir `Idempotency-Key`.
- Reservas eternas: siempre llenar `expira_en`.
- Compensación doble: verificar estado antes de cambiar.
