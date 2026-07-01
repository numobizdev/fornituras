-- Contenido configurable de la landing (016). Una fila = una sección de una de las dos caras
-- (scope PUBLIC/HOME) con un tipo (HERO/ANNOUNCEMENT/QUICK_LINKS/RICH_TEXT). Todo el texto se
-- almacena literal (el escape ocurre en el render, Angular) y las URLs se validan en el borde
-- (esquema http/https o ruta interna). Los accesos rápidos se guardan como JSON en config_json.
-- Baja lógica (active). Sin PII: la cara pública solo contiene contenido institucional/de marca.
-- Nota de numeración: la spec preveía V19, ya ocupado por 008 (incident); se toma la siguiente libre.
-- Decisión registrada en el ADR 0015 (docs/04-decisiones/0015-landing-configurable.md).

CREATE TABLE landing_section (
    id BIGINT IDENTITY(1,1) NOT NULL,
    scope NVARCHAR(10) NOT NULL,
    type NVARCHAR(20) NOT NULL,
    titulo NVARCHAR(160) NULL,
    subtitulo NVARCHAR(240) NULL,
    cuerpo NVARCHAR(2000) NULL,
    imagen_url NVARCHAR(512) NULL,
    cta_label NVARCHAR(80) NULL,
    cta_url NVARCHAR(512) NULL,
    orden INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    config_json NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_landing_section PRIMARY KEY (id),
    CONSTRAINT ck_landing_scope CHECK (scope IN ('PUBLIC', 'HOME')),
    CONSTRAINT ck_landing_type CHECK (type IN ('HERO', 'ANNOUNCEMENT', 'QUICK_LINKS', 'RICH_TEXT'))
);

CREATE INDEX idx_landing_scope_active_orden ON landing_section (scope, active, orden);

-- Seed por defecto para que ninguna cara aparezca vacía al arrancar (SC-013 / assumptions de la spec).
INSERT INTO landing_section (scope, type, titulo, subtitulo, cuerpo, imagen_url, cta_label, cta_url, orden, active, config_json)
VALUES
    ('PUBLIC', 'HERO',
     'Sistema de Gestión de Blindajes',
     'Acceso institucional',
     NULL, NULL, 'Acceder', '/login', 0, 1, NULL),

    ('HOME', 'HERO',
     'Bienvenido a SIGEFOR',
     'Panel de gestión de blindajes y dotación',
     NULL, NULL, NULL, NULL, 0, 1, NULL),

    ('HOME', 'ANNOUNCEMENT',
     'Aviso',
     NULL,
     'Mantén al día el inventario y las asignaciones para reflejar la disponibilidad real del equipo.',
     NULL, NULL, NULL, 1, 1, NULL),

    ('HOME', 'QUICK_LINKS',
     'Accesos rápidos',
     NULL, NULL, NULL, NULL, NULL, 2, 1,
     N'[{"label":"Elementos","url":"/elementos","icon":"people-outline"},{"label":"Fornituras","url":"/fornituras","icon":"cube-outline"},{"label":"Asignación","url":"/asignacion","icon":"link-outline"}]');
