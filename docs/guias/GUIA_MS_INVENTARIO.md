# 📦 Guía Día 1 — ms-inventario

Responsable: Eric  
Puerto: `8083`  
Base de datos: `db_inventario`  
Consumer group: `grupo-inventario`

## 1. 🎯 Propósito del servicio

`ms-inventario` administra secciones, cupos, mesas y asientos. Es crítico porque protege contra la sobreventa: nunca debe permitir vender más entradas que la capacidad real. Se relaciona con `ms-eventos` al consumir `evento.creado`, con `ms-reservas` al reservar/liberar cupos y con `ms-pagos` al convertir reservas en ventas confirmadas.

## 2. ✅ Tareas del Día 1

Crear el proyecto desde https://start.spring.io:

- Project: Maven
- Java: 17
- Spring Boot: 3.x
- Group: `com.soldout`
- Artifact: `ms-inventario`
- Package: `com.soldout.inventario`

Dependencias desde Spring Initializr:

- Spring Web
- Spring Data JPA
- Spring Boot Actuator
- Spring Boot Validation
- Lombok

Agregar manualmente al `pom.xml`:

```xml
<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
<dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
<dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
```

## 3. 📁 Estructura requerida

```text
ms-inventario/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/soldout/inventario/
    │   ├── MsInventarioApplication.java
    │   ├── config/RedisConfig.java
    │   ├── controller/InventarioController.java
    │   ├── dto/RespuestaApi.java
    │   ├── entity/Seccion.java
    │   ├── entity/InventarioSeccion.java
    │   ├── entity/Mesa.java
    │   ├── entity/Asiento.java
    │   ├── repository/SeccionRepository.java
    │   ├── repository/InventarioSeccionRepository.java
    │   ├── repository/MesaRepository.java
    │   ├── repository/AsientoRepository.java
    │   ├── service/InventarioService.java
    │   └── kafka/InventarioKafkaConsumer.java
    └── resources/
        ├── application.yml
        └── application-docker.yml
```

## 4. ⚙️ application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: ms-inventario
  datasource:
    url: jdbc:postgresql://localhost:5432/db_inventario
    username: admin
    password: admin123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: grupo-inventario
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

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
```

La sección `datasource` conecta a `db_inventario`. `ddl-auto: validate` evita recrear tablas. Kafka queda listo para consumir eventos. Redis se usa para disponibilidad rápida.

## 5. 🐳 application-docker.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DB_INVENTARIO}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
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
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 7. 🧱 Entidades JPA

### Seccion.java

```java
@Entity
@Table(name = "secciones", uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "nombre"}))
public class Seccion {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(name="evento_id", nullable=false) private UUID eventoId;
  @Column(nullable=false, length=200) private String nombre;
  @Column(nullable=false, length=20) private String tipo;
  @Column(name="capacidad_total", nullable=false) private Integer capacidadTotal;
  @Column(nullable=false, precision=10, scale=2) private BigDecimal precio;
  private String descripcion;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
}
```

`UNIQUE(evento_id,nombre)` impide duplicar secciones con el mismo nombre en un evento. `tipo` debe ser `GENERAL` o `NUMERADO`.

### InventarioSeccion.java

```java
@Entity
@Table(name = "inventario_secciones")
public class InventarioSeccion {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @OneToOne @JoinColumn(name="seccion_id", nullable=false, unique=true) private Seccion seccion;
  @Column(name="total_asientos", nullable=false) private Integer totalAsientos;
  @Column(name="asientos_disponibles", nullable=false) private Integer asientosDisponibles;
  @Column(name="asientos_reservados", nullable=false) private Integer asientosReservados = 0;
  @Column(name="asientos_vendidos", nullable=false) private Integer asientosVendidos = 0;
  @Version private Long version;
  @Column(name="actualizado_en", nullable=false) private LocalDateTime actualizadoEn = LocalDateTime.now();
}
```

`@Version` ayuda con optimistic locking. La DB además valida que disponibles + reservados + vendidos = total.

### Mesa.java

```java
@Entity
@Table(name = "mesas", uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_id", "nombre"}))
public class Mesa {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @ManyToOne @JoinColumn(name="seccion_id", nullable=false) private Seccion seccion;
  @Column(nullable=false, length=100) private String nombre;
  @Column(nullable=false) private Integer capacidad;
  @Column(nullable=false, length=20) private String estado = "DISPONIBLE";
}
```

### Asiento.java

```java
@Entity
@Table(name = "asientos", uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_id", "numero_asiento"}))
public class Asiento {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @ManyToOne @JoinColumn(name="seccion_id", nullable=false) private Seccion seccion;
  @ManyToOne @JoinColumn(name="mesa_id") private Mesa mesa;
  @Column(name="numero_asiento", nullable=false, length=20) private String numeroAsiento;
  @Column(length=10) private String fila;
  @Column(nullable=false, length=20) private String estado = "DISPONIBLE";
  @Column(name="reserva_id") private UUID reservaId;
  @Column(name="reservado_en") private LocalDateTime reservadoEn;
}
```

## 8. 🗃️ Repositorios

```java
public interface SeccionRepository extends JpaRepository<Seccion, UUID> {
  List<Seccion> findByEventoId(UUID eventoId);
  Optional<Seccion> findByEventoIdAndNombre(UUID eventoId, String nombre);
}
```

```java
public interface InventarioSeccionRepository extends JpaRepository<InventarioSeccion, UUID> {
  Optional<InventarioSeccion> findBySeccionId(UUID seccionId);
  @Modifying
  @Query("update InventarioSeccion i set i.asientosDisponibles = i.asientosDisponibles - :cantidad, i.asientosReservados = i.asientosReservados + :cantidad where i.seccion.id = :seccionId and i.asientosDisponibles >= :cantidad")
  int reservarDisponibles(UUID seccionId, int cantidad);
}
```

```java
public interface MesaRepository extends JpaRepository<Mesa, UUID> {
  List<Mesa> findBySeccionId(UUID seccionId);
}
public interface AsientoRepository extends JpaRepository<Asiento, UUID> {
  List<Asiento> findBySeccionId(UUID seccionId);
  List<Asiento> findByReservaId(UUID reservaId);
}
```

## 9. 🔴 Redis

Clave principal:

```text
inventario:disponible:{seccionId} -> Integer, sin TTL
```

Configuración:

```java
@Configuration
public class RedisConfig {
  @Bean
  public RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Integer> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
    return template;
  }
}
```

## 10. 📨 Eventos Kafka a preparar

- `evento.creado`: crear secciones e inventario inicial.
- `pago.confirmado`: pasar reservados a vendidos.
- `reserva.expirada`: liberar reservados.

## 11. 🛡️ Anti-sobreventa

```sql
UPDATE inventario_secciones
SET asientos_disponibles = asientos_disponibles - :cantidad,
    asientos_reservados = asientos_reservados + :cantidad
WHERE seccion_id = :seccionId
  AND asientos_disponibles >= :cantidad;
```

Si retorna 0 filas, no hay cupo suficiente.

## 12. ✅ Criterios de éxito Día 1

- `docker-compose up -d --build ms-inventario` levanta.
- `GET http://localhost:8083/actuator/health` responde `UP`.
- PostgreSQL muestra `secciones`, `inventario_secciones`, `mesas`, `asientos`.
- La app usa `ddl-auto: validate`.

## 13. 🧪 Cómo levantar y probar

```bash
docker-compose up -d postgres redis kafka
docker-compose up -d --build ms-inventario
curl http://localhost:8083/actuator/health
docker exec soldout-postgres psql -U admin -d db_inventario -c "\dt"
```

## 14. ⚠️ Errores comunes

- `relation does not exist`: revisar que `database/init.sql` haya corrido.
- `Connection refused Redis`: levantar `redis`.
- `No resolvable bootstrap`: dentro de Docker usar `kafka:29092`.
- Sobreventa en pruebas: usar `UPDATE` condicional, no leer-modificar-guardar sin lock.
