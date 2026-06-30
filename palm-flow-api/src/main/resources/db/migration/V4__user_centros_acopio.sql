IF OBJECT_ID(N'dbo.user_centros_acopio', N'U') IS NULL
    CREATE TABLE dbo.user_centros_acopio (
        user_id             BIGINT NOT NULL,
        centro_acopio_uuid  UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT pk_user_centros_acopio PRIMARY KEY (user_id, centro_acopio_uuid),
        CONSTRAINT fk_uca_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
        CONSTRAINT fk_uca_centro FOREIGN KEY (centro_acopio_uuid) REFERENCES dbo.centros_acopio (uuid)
    );
