-- Padrón de elementos policiales (003). PII de ALTA SENSIBILIDAD.
-- Las columnas nombre/apellidos/curp/rfc se persisten CIFRADAS a nivel de aplicación (AES-GCM,
-- ADR 0006); por eso son NVARCHAR amplias (texto base64 del ciphertext) y no se indexan por
-- contenido. La igualdad de curp/rfc se resuelve con blind index (curp_idx/rfc_idx, HMAC).
-- La placa es identificador operativo: en claro, única y normalizada. Los catálogos van en claro.

CREATE TABLE sexo (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(30) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_sexo PRIMARY KEY (id),
    CONSTRAINT uk_sexo_nombre UNIQUE (nombre)
);

CREATE TABLE tipo_sangre (
    id BIGINT IDENTITY(1,1) NOT NULL,
    etiqueta NVARCHAR(5) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_tipo_sangre PRIMARY KEY (id),
    CONSTRAINT uk_tipo_sangre_etiqueta UNIQUE (etiqueta)
);

INSERT INTO sexo (nombre) VALUES ('MASCULINO'), ('FEMENINO');

INSERT INTO tipo_sangre (etiqueta) VALUES
    ('O+'), ('O-'), ('A+'), ('A-'), ('B+'), ('B-'), ('AB+'), ('AB-');

CREATE TABLE officers (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(512) NOT NULL,
    apellido_paterno NVARCHAR(512) NOT NULL,
    apellido_materno NVARCHAR(512) NULL,
    placa NVARCHAR(40) NOT NULL,
    placa_normalizada NVARCHAR(40) NOT NULL,
    curp NVARCHAR(512) NULL,
    curp_idx NVARCHAR(64) NULL,
    rfc NVARCHAR(512) NULL,
    rfc_idx NVARCHAR(64) NULL,
    sexo_id BIGINT NOT NULL,
    tipo_sangre_id BIGINT NULL,
    municipio_id BIGINT NOT NULL,
    foto_url NVARCHAR(500) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_officers PRIMARY KEY (id),
    CONSTRAINT uk_officers_placa_norm UNIQUE (placa_normalizada),
    CONSTRAINT fk_officers_sexo FOREIGN KEY (sexo_id) REFERENCES sexo(id),
    CONSTRAINT fk_officers_tipo_sangre FOREIGN KEY (tipo_sangre_id) REFERENCES tipo_sangre(id),
    CONSTRAINT fk_officers_municipio FOREIGN KEY (municipio_id) REFERENCES municipio(id)
);

-- Únicos filtrados: la CURP/RFC son únicas cuando están presentes, pero pueden ser NULL
-- (varios elementos sin CURP capturada conviven sin chocar).
CREATE UNIQUE INDEX uk_officers_curp_idx ON officers (curp_idx) WHERE curp_idx IS NOT NULL;
CREATE UNIQUE INDEX uk_officers_rfc_idx ON officers (rfc_idx) WHERE rfc_idx IS NOT NULL;

CREATE INDEX idx_officers_municipio ON officers (municipio_id);
CREATE INDEX idx_officers_sexo ON officers (sexo_id);
CREATE INDEX idx_officers_active ON officers (active);
