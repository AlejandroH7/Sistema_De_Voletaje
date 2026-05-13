# 🔔 Guía Día 1 — ms-notificaciones

Responsable: Eric  
Puerto: `8086`  
Base de datos: `db_notificaciones`  
Consumer group: `grupo-notificaciones`

## 1. 🎯 Propósito del servicio

`ms-notificaciones` registra y simula el envío de comunicaciones a usuarios. Consume eventos de pagos y reservas para informar confirmaciones, expiraciones y fallos. La deduplicación es obligatoria porque Kafka puede entregar más de una vez; enviar dos emails por el mismo pago deteriora la experiencia y causa confusión.

La idempotencia se logra con `clave_idempotencia UNIQUE`: cada evento genera una clave determinística. Si ya existe, se ignora.

## 2. ✅ Tareas Día 1

Crear Spring Boot con:

- Spring Web
- Spring Data JPA
- Spring Boot Actuator
- Spring Boot Validation
- Lombok
- PostgreSQL driver
- Spring Kafka
- Micrometer Prometheus

## 3. 📁 Estructura requerida

```text
ms-notificaciones/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/soldout/notificaciones/
    │   ├── MsNotificacionesApplication.java
    │   ├── controller/NotificacionController.java
    │   ├── dto/RespuestaApi.java
    │   ├── entity/Notificacion.java
    │   ├── repository/NotificacionRepository.java
    │   ├── service/NotificacionService.java
    │   └── kafka/NotificacionKafkaConsumer.java
    └── resources/application.yml
    └── resources/application-docker.yml
```

## 4. ⚙️ application.yml

```yaml
server:
  port: 8086
spring:
  application:
    name: ms-notificaciones
  datasource:
    url: jdbc:postgresql://localhost:5432/db_notificaciones
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
      group-id: grupo-notificaciones
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
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
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DB_NOTIFICACIONES}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

## 6. 🐳 Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 7. 🧱 Entidad Notificacion

```java
@Entity
@Table(name = "notificaciones")
public class Notificacion {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(name="usuario_id", nullable=false) private UUID usuarioId;
  @Column(name="reserva_id") private UUID reservaId;
  @Column(nullable=false, length=50) private String tipo;
  @Column(nullable=false, length=20) private String canal = "CORREO";
  @Column(length=255) private String asunto;
  private String contenido;
  @Column(nullable=false, length=20) private String estado = "PENDIENTE";
  @Column(name="clave_idempotencia", nullable=false, unique=true, length=64) private String claveIdempotencia;
  @Column(name="enviado_en") private LocalDateTime enviadoEn;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
}
```

Tipos válidos: `RESERVA_CONFIRMADA`, `RESERVA_EXPIRADA`, `PAGO_FALLIDO`. `clave_idempotencia` es `UNIQUE` para evitar envíos duplicados.

## 8. 🗃️ Repositorio

```java
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
  boolean existsByClaveIdempotencia(String claveIdempotencia);
  List<Notificacion> findByUsuarioId(UUID usuarioId);
}
```

## 9. 🔁 Deduplicación

1. Recibir evento Kafka.
2. Calcular `clave_idempotencia = sha256(evento_id + tipo + usuario_id)`.
3. Consultar `existsByClaveIdempotencia`.
4. Si existe, loguear e ignorar.
5. Si no existe, guardar notificación.
6. Simular envío.
7. Marcar `ENVIADO` o `FALLIDO`.

## 10. 📨 Eventos consumidos

| Topic | Tipo generado |
|---|---|
| pago.confirmado | RESERVA_CONFIRMADA |
| pago.fallido | PAGO_FALLIDO |
| reserva.expirada | RESERVA_EXPIRADA |
| reserva.confirmada | RESERVA_CONFIRMADA con QR simulado |

## 11. ✅ Criterios de éxito Día 1

- Servicio levanta en `8086`.
- Health `UP`.
- Tabla `notificaciones` visible.
- Repository compila.
- Consumer class existe aunque aún no procese lógica final.

## 12. 🧪 Levantar y probar

```bash
docker-compose up -d postgres kafka
docker-compose up -d --build ms-notificaciones
curl http://localhost:8086/actuator/health
docker exec soldout-postgres psql -U admin -d db_notificaciones -c "\dt"
```

## 13. ⚠️ Errores comunes

- Método mal escrito `existsByClaveidempotencia`: debe coincidir con el nombre Java del campo. Recomendado: `claveIdempotencia`.
- Kafka no conecta: usar `kafka:29092` en Docker.
- Duplicados: no generar clave con timestamp; debe ser determinística.
