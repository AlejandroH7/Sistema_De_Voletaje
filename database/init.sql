-- =============================================================================
--  SOLD-OUT CHALLENGE LIVE
--  Script de inicialización de bases de datos
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
--  1. db_usuarios
-- =============================================================================
CREATE DATABASE db_usuarios WITH ENCODING = 'UTF8';
\connect db_usuarios
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE usuarios (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre          VARCHAR(200) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    telefono        VARCHAR(20),
    rol             VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE',
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_email    UNIQUE (email),
    CONSTRAINT ck_usuarios_rol      CHECK (rol    IN ('CLIENTE','ORGANIZADOR','ADMIN')),
    CONSTRAINT ck_usuarios_estado   CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO'))
);

CREATE INDEX idx_usuarios_email  ON usuarios (email);
CREATE INDEX idx_usuarios_rol    ON usuarios (rol);
CREATE INDEX idx_usuarios_estado ON usuarios (estado);

-- =============================================================================
--  2. db_eventos
-- =============================================================================
CREATE DATABASE db_eventos WITH ENCODING = 'UTF8';
\connect db_eventos
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE lugares (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre           VARCHAR(200) NOT NULL,
    direccion        TEXT,
    ciudad           VARCHAR(100),
    capacidad_maxima INT          NOT NULL CHECK (capacidad_maxima > 0),
    creado_en        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE eventos (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre         VARCHAR(255) NOT NULL,
    descripcion    TEXT,
    lugar_id       UUID         NOT NULL REFERENCES lugares(id),
    fecha_evento   TIMESTAMP    NOT NULL,
    tipo_evento    VARCHAR(20)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    creado_en      TIMESTAMP    NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_eventos_tipo   CHECK (tipo_evento IN ('SOLO_NUMERADO','SOLO_GENERAL','MIXTO')),
    CONSTRAINT ck_eventos_estado CHECK (estado IN ('BORRADOR','ACTIVO','AGOTADO','CANCELADO'))
);

CREATE INDEX idx_eventos_lugar_id ON eventos (lugar_id);
CREATE INDEX idx_eventos_estado   ON eventos (estado);
CREATE INDEX idx_eventos_fecha    ON eventos (fecha_evento);
CREATE INDEX idx_eventos_tipo     ON eventos (tipo_evento);

-- =============================================================================
--  3. db_inventario
-- =============================================================================
CREATE DATABASE db_inventario WITH ENCODING = 'UTF8';
\connect db_inventario
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE secciones (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    evento_id       UUID           NOT NULL,
    nombre          VARCHAR(200)   NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    capacidad_total INT            NOT NULL CHECK (capacidad_total > 0),
    precio          NUMERIC(10,2)  NOT NULL CHECK (precio >= 0),
    descripcion     TEXT,
    creado_en       TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_secciones_evento_nombre UNIQUE (evento_id, nombre),
    CONSTRAINT ck_secciones_tipo          CHECK (tipo IN ('GENERAL','NUMERADO'))
);

CREATE TABLE inventario_secciones (
    id                   UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
    seccion_id           UUID      NOT NULL UNIQUE REFERENCES secciones(id),
    total_asientos       INT       NOT NULL CHECK (total_asientos > 0),
    asientos_disponibles INT       NOT NULL DEFAULT 0,
    asientos_reservados  INT       NOT NULL DEFAULT 0,
    asientos_vendidos    INT       NOT NULL DEFAULT 0,
    version              BIGINT    NOT NULL DEFAULT 0,
    actualizado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inventario_disponibles CHECK (asientos_disponibles >= 0),
    CONSTRAINT ck_inventario_reservados  CHECK (asientos_reservados  >= 0),
    CONSTRAINT ck_inventario_vendidos    CHECK (asientos_vendidos    >= 0),
    CONSTRAINT ck_inventario_suma        CHECK (
        asientos_disponibles + asientos_reservados + asientos_vendidos = total_asientos
    )
);

CREATE TABLE mesas (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    seccion_id UUID         NOT NULL REFERENCES secciones(id),
    nombre     VARCHAR(100) NOT NULL,
    capacidad  INT          NOT NULL CHECK (capacidad > 0),
    estado     VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT uq_mesas_seccion_nombre UNIQUE (seccion_id, nombre),
    CONSTRAINT ck_mesas_estado         CHECK (estado IN ('DISPONIBLE','PARCIAL','RESERVADA','VENDIDA'))
);

CREATE TABLE asientos (
    id             UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    seccion_id     UUID        NOT NULL REFERENCES secciones(id),
    mesa_id        UUID        REFERENCES mesas(id),
    numero_asiento VARCHAR(20) NOT NULL,
    fila           VARCHAR(10),
    estado         VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    reserva_id     UUID,
    reservado_en   TIMESTAMP,
    CONSTRAINT uq_asientos_seccion_numero UNIQUE (seccion_id, numero_asiento),
    CONSTRAINT ck_asientos_estado         CHECK (estado IN ('DISPONIBLE','RESERVADO','VENDIDO'))
);

CREATE INDEX idx_secciones_evento_id   ON secciones           (evento_id);
CREATE INDEX idx_secciones_tipo        ON secciones           (tipo);
CREATE INDEX idx_inventario_seccion_id ON inventario_secciones (seccion_id);
CREATE INDEX idx_mesas_seccion_id      ON mesas               (seccion_id);
CREATE INDEX idx_asientos_seccion_id   ON asientos            (seccion_id);
CREATE INDEX idx_asientos_mesa_id      ON asientos            (mesa_id);
CREATE INDEX idx_asientos_estado       ON asientos            (estado);
CREATE INDEX idx_asientos_reserva_id   ON asientos            (reserva_id);

-- =============================================================================
--  4. db_reservas
-- =============================================================================
CREATE DATABASE db_reservas WITH ENCODING = 'UTF8';
\connect db_reservas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE reservas (
    id                 UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id         UUID           NOT NULL,
    evento_id          UUID           NOT NULL,
    precio_total       NUMERIC(10,2)  NOT NULL CHECK (precio_total > 0),
    estado             VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    clave_idempotencia VARCHAR(64)    NOT NULL,
    expira_en          TIMESTAMP      NOT NULL,
    confirmado_en      TIMESTAMP,
    creado_en          TIMESTAMP      NOT NULL DEFAULT NOW(),
    actualizado_en     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reservas_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_reservas_estado       CHECK (estado IN ('PENDIENTE','CONFIRMADO','EXPIRADO','CANCELADO'))
);

CREATE TABLE detalle_reserva (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    reserva_id      UUID           NOT NULL REFERENCES reservas(id),
    seccion_id      UUID           NOT NULL,
    tipo_asiento    VARCHAR(20)    NOT NULL,
    asiento_id      UUID,
    mesa_id         UUID,
    numero_asiento  VARCHAR(20),
    nombre_seccion  VARCHAR(200)   NOT NULL,
    cantidad        INT            NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2)  NOT NULL,
    CONSTRAINT ck_detalle_tipo CHECK (tipo_asiento IN ('GENERAL','NUMERADO'))
);

CREATE INDEX idx_reservas_usuario_id        ON reservas        (usuario_id);
CREATE INDEX idx_reservas_evento_id         ON reservas        (evento_id);
CREATE INDEX idx_reservas_estado            ON reservas        (estado);
CREATE INDEX idx_reservas_expira_en         ON reservas        (expira_en);
CREATE INDEX idx_reservas_idempotencia      ON reservas        (clave_idempotencia);
CREATE INDEX idx_detalle_reserva_id         ON detalle_reserva (reserva_id);
CREATE INDEX idx_detalle_seccion_id         ON detalle_reserva (seccion_id);

-- =============================================================================
--  5. db_pagos
-- =============================================================================
CREATE DATABASE db_pagos WITH ENCODING = 'UTF8';
\connect db_pagos
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE pagos (
    id                 UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    reserva_id         UUID           NOT NULL,
    usuario_id         UUID           NOT NULL,
    monto              NUMERIC(10,2)  NOT NULL CHECK (monto > 0),
    moneda             VARCHAR(3)     NOT NULL DEFAULT 'GTQ',
    estado             VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    clave_idempotencia VARCHAR(64)    NOT NULL,
    metodo_pago        VARCHAR(50)    NOT NULL DEFAULT 'TARJETA',
    motivo_fallo       TEXT,
    procesado_en       TIMESTAMP,
    creado_en          TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pagos_reserva      UNIQUE (reserva_id),
    CONSTRAINT uq_pagos_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_pagos_estado       CHECK (estado IN ('PENDIENTE','COMPLETADO','FALLIDO','REEMBOLSADO'))
);

CREATE TABLE pagos_salida (
    id           UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_agregado  UUID         NOT NULL,
    tipo_evento  VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    publicado    BOOLEAN      NOT NULL DEFAULT FALSE,
    publicado_en TIMESTAMP,
    creado_en    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pagos_reserva_id         ON pagos        (reserva_id);
CREATE INDEX idx_pagos_usuario_id         ON pagos        (usuario_id);
CREATE INDEX idx_pagos_estado             ON pagos        (estado);
CREATE INDEX idx_pagos_idempotencia       ON pagos        (clave_idempotencia);
CREATE INDEX idx_salida_no_publicados     ON pagos_salida (publicado, creado_en)
    WHERE publicado = FALSE;

-- =============================================================================
--  6. db_notificaciones
-- =============================================================================
CREATE DATABASE db_notificaciones WITH ENCODING = 'UTF8';
\connect db_notificaciones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE notificaciones (
    id                 UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id         UUID        NOT NULL,
    reserva_id         UUID,
    tipo               VARCHAR(50) NOT NULL,
    canal              VARCHAR(20) NOT NULL DEFAULT 'CORREO',
    asunto             VARCHAR(255),
    contenido          TEXT,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    clave_idempotencia VARCHAR(64) NOT NULL,
    enviado_en         TIMESTAMP,
    creado_en          TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notif_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_notif_tipo   CHECK (tipo   IN ('RESERVA_CONFIRMADA','RESERVA_EXPIRADA','PAGO_FALLIDO')),
    CONSTRAINT ck_notif_canal  CHECK (canal  IN ('CORREO','SMS','PUSH')),
    CONSTRAINT ck_notif_estado CHECK (estado IN ('PENDIENTE','ENVIADO','FALLIDO'))
);

CREATE INDEX idx_notif_usuario_id    ON notificaciones (usuario_id);
CREATE INDEX idx_notif_reserva_id    ON notificaciones (reserva_id);
CREATE INDEX idx_notif_tipo          ON notificaciones (tipo);
CREATE INDEX idx_notif_estado        ON notificaciones (estado);
CREATE INDEX idx_notif_idempotencia  ON notificaciones (clave_idempotencia);
