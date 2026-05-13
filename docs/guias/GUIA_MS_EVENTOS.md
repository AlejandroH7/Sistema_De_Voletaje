# 🎤 Guía Día 1 — ms-eventos

Responsable: Josue  
Puerto: `8082`  
Base: `db_eventos`  
Producer Kafka: `evento.creado`

## 1. 🎯 Propósito

`ms-eventos` administra lugares y eventos. Al publicar un evento envía `evento.creado` para que `ms-inventario` cree secciones e inventario inicial. Flujo: crear evento en `BORRADOR` → publicar → Kafka → inventario inicializado.

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
ms-eventos/src/main/java/com/soldout/eventos/
├── MsEventosApplication.java
├── controller/EventoController.java
├── dto/CrearEventoRequest.java
├── dto/EventoResponse.java
├── dto/RespuestaApi.java
├── entity/Lugar.java
├── entity/Evento.java
├── repository/LugarRepository.java
├── repository/EventoRepository.java
├── service/EventoService.java
└── kafka/EventoKafkaProducer.java
```

## 4. ⚙️ application.yml

```yaml
server:
  port: 8082
spring:
  application:
    name: ms-eventos
  datasource:
    url: jdbc:postgresql://localhost:5432/db_eventos
    username: admin
    password: admin123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
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
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DB_EVENTOS}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

## 6. 🐳 Dockerfile

Usar estándar con `EXPOSE 8082`.

## 7. 🧱 Entidades

### Lugar

```java
@Entity
@Table(name = "lugares")
public class Lugar {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(nullable=false, length=200) private String nombre;
  private String direccion;
  @Column(length=100) private String ciudad;
  @Column(name="capacidad_maxima", nullable=false) private Integer capacidadMaxima;
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
}
```

### Evento

```java
@Entity
@Table(name = "eventos")
public class Evento {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @Column(nullable=false, length=255) private String nombre;
  private String descripcion;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="lugar_id", nullable=false) private Lugar lugar;
  @Column(name="fecha_evento", nullable=false) private LocalDateTime fechaEvento;
  @Column(name="tipo_evento", nullable=false, length=20) private String tipoEvento;
  @Column(nullable=false, length=20) private String estado = "BORRADOR";
  @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn = LocalDateTime.now();
  @Column(name="actualizado_en", nullable=false) private LocalDateTime actualizadoEn = LocalDateTime.now();
}
```

`ManyToOne` permite navegar desde evento hacia lugar y mantiene integridad referencial JPA. Usar solo UUID obligaría a consultas manuales y perdería parte del modelo relacional.

## 8. 🗃️ Repositorios

```java
public interface LugarRepository extends JpaRepository<Lugar, UUID> {}
public interface EventoRepository extends JpaRepository<Evento, UUID> {
  List<Evento> findByEstado(String estado);
  List<Evento> findByLugarId(UUID lugarId);
  boolean existsByNombreAndFechaEvento(String nombre, LocalDateTime fechaEvento);
}
```

## 9. 📦 DTOs

`CrearEventoRequest`: `nombre`, `descripcion`, `lugar_id`, `fecha_evento`, `tipo_evento`, `secciones`.

Las secciones van en creación para que al publicar el evento se mande un único payload a inventario.

```json
{
  "nombre": "Festival Sold Out",
  "descripcion": "Evento masivo",
  "lugar_id": "uuid",
  "fecha_evento": "2026-08-01T20:00:00",
  "tipo_evento": "MIXTO",
  "secciones": [
    {"nombre":"Dance Floor","tipo":"GENERAL","capacidad":30000,"precio":350.00}
  ]
}
```

## 10. 🌐 Endpoints Día 2

- `POST /api/eventos`: crear evento. Rol `ORGANIZADOR`.
- `GET /api/eventos`: listar activos. Público.
- `GET /api/eventos/{id}`: detalle. Público.
- `PUT /api/eventos/{id}/publicar`: publicar.

## 11. 📨 Evento Kafka

Topic `evento.creado`:

```json
{
  "evento_id": "uuid",
  "tipo_evento": "MIXTO",
  "secciones": [
    {"nombre": "Dance Floor", "tipo": "GENERAL", "capacidad": 30000, "precio": 350.00},
    {"nombre": "Palco VIP", "tipo": "NUMERADO", "capacidad": 50, "precio": 2500.00}
  ]
}
```

## 12. ✅ Criterios Día 1

- Proyecto compila.
- Health en `8082`.
- Entidades validan contra DB.
- Repositorios existen.

## 13. 🧪 Probar

```bash
docker-compose up -d postgres kafka
docker-compose up -d --build ms-eventos
curl http://localhost:8082/actuator/health
```

## 14. ⚠️ Errores comunes

- `LazyInitializationException`: convertir entidad a DTO dentro de transacción.
- Estado inválido: validar antes de guardar.
- Publicar dos veces: validar transición `BORRADOR -> ACTIVO`.
