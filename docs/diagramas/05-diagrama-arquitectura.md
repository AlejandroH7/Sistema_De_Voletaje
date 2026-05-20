# 5. Diagrama de arquitectura

```mermaid
flowchart LR
    cliente["Cliente Web/Movil"]
    postman["Postman\npruebas manuales"]

    subgraph compose["Docker Compose - despliegue local"]
        gateway["api-gateway :8080"]
        usuarios["ms-usuarios :8081"]
        eventos["ms-eventos :8082"]
        inventario["ms-inventario :8083"]
        reservas["ms-reservas :8084"]
        pagos["ms-pagos :8085"]
        notificaciones["ms-notificaciones :8086"]

        subgraph datos["Persistencia por dominio"]
            dbu["PostgreSQL\ndb_usuarios"]
            dbe["PostgreSQL\ndb_eventos"]
            dbi["PostgreSQL\ndb_inventario"]
            dbr["PostgreSQL\ndb_reservas"]
            dbp["PostgreSQL\ndb_pagos"]
            dbn["PostgreSQL\ndb_notificaciones"]
        end

        redis["Redis\ncache inventario + TTL reservas"]
        kafka["Kafka\nmensajeria asincrona"]
        prometheus["Prometheus :9090"]
        grafana["Grafana :3000"]
    end

    cliente -->|"HTTP + JWT"| gateway
    postman -->|"HTTP + JWT / pruebas"| gateway

    gateway -->|"rutas + headers internos"| usuarios
    gateway -->|"rutas + headers internos"| eventos
    gateway -->|"rutas + headers internos"| inventario
    gateway -->|"rutas + headers internos"| reservas
    gateway -->|"rutas + headers internos"| pagos
    gateway -->|"rutas + headers internos"| notificaciones

    usuarios --> dbu
    eventos --> dbe
    inventario --> dbi
    reservas --> dbr
    pagos --> dbp
    notificaciones --> dbn

    inventario <--> redis
    reservas <--> redis

    usuarios --> kafka
    eventos --> kafka
    reservas <--> kafka
    pagos --> kafka
    kafka --> inventario
    kafka --> notificaciones

    prometheus -.->|"scrape /actuator/prometheus"| gateway
    prometheus -.->|"scrape /actuator/prometheus"| usuarios
    prometheus -.->|"scrape /actuator/prometheus"| eventos
    prometheus -.->|"scrape /actuator/prometheus"| inventario
    prometheus -.->|"scrape /actuator/prometheus"| reservas
    prometheus -.->|"scrape /actuator/prometheus"| pagos
    prometheus -.->|"scrape /actuator/prometheus"| notificaciones
    grafana -->|"consulta metricas"| prometheus
```

La solución centraliza la entrada en `api-gateway`, que valida JWT e inyecta headers internos antes de enrutar a los microservicios. Cada dominio persiste en su propia base PostgreSQL, mientras Redis acelera disponibilidad y conserva TTL de reservas.

Kafka desacopla los flujos de eventos entre servicios y Prometheus obtiene métricas de los siete servicios para que Grafana construya dashboards operativos. Todo el entorno local se orquesta desde Docker Compose.
