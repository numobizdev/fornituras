using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Fornituras.Api.Data.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "assignment",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    equipment_id = table.Column<long>(type: "bigint", nullable: false),
                    officer_id = table.Column<long>(type: "bigint", nullable: false),
                    fecha_asignacion = table.Column<DateTime>(type: "datetime2", nullable: false),
                    fecha_devolucion = table.Column<DateTime>(type: "datetime2", nullable: true),
                    asignado_por = table.Column<long>(type: "bigint", nullable: true),
                    recibido_por = table.Column<long>(type: "bigint", nullable: true),
                    firma_url = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    observaciones = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_assignment", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "audit_log",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    usuario_id = table.Column<long>(type: "bigint", nullable: true),
                    actor = table.Column<string>(type: "nvarchar(150)", maxLength: 150, nullable: true),
                    accion = table.Column<string>(type: "nvarchar(80)", maxLength: 80, nullable: false),
                    entidad = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: true),
                    entidad_id = table.Column<long>(type: "bigint", nullable: true),
                    occurred_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    ip = table.Column<string>(type: "nvarchar(45)", maxLength: 45, nullable: true),
                    evidencia = table.Column<string>(type: "nvarchar(1000)", maxLength: 1000, nullable: true),
                    prev_hash = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_audit_log", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "catalog",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    code = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    nombre = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    descripcion = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    is_system = table.Column<bool>(type: "bit", nullable: false, defaultValue: false),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_catalog", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "decommission",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    equipment_id = table.Column<long>(type: "bigint", nullable: false),
                    motivo_id = table.Column<long>(type: "bigint", nullable: false),
                    fecha = table.Column<DateOnly>(type: "date", nullable: false),
                    responsable = table.Column<long>(type: "bigint", nullable: true),
                    observaciones = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_decommission", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "decommission_reason",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    nombre = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_decommission_reason", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "equipment",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    codigo_qr = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: false),
                    codigo_normalizado = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: false),
                    equipment_type_id = table.Column<long>(type: "bigint", nullable: false),
                    size_id = table.Column<long>(type: "bigint", nullable: true),
                    warehouse_id = table.Column<long>(type: "bigint", nullable: false),
                    status = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false, defaultValue: "DISPONIBLE"),
                    descripcion = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: true),
                    marca = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    modelo = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    nivel_balistico = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: true),
                    numero_inventario = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: true),
                    fecha_fabricacion = table.Column<DateOnly>(type: "date", nullable: true),
                    fecha_adquisicion = table.Column<DateOnly>(type: "date", nullable: true),
                    vida_util_meses = table.Column<int>(type: "int", nullable: true),
                    fecha_vencimiento = table.Column<DateOnly>(type: "date", nullable: true),
                    observaciones = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    foto_url = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_equipment", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "incident",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    equipment_id = table.Column<long>(type: "bigint", nullable: false),
                    tipo = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false),
                    descripcion = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: false),
                    estado = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false, defaultValue: "ABIERTA"),
                    fecha_reporte = table.Column<DateTime>(type: "datetime2", nullable: false),
                    fecha_resolucion = table.Column<DateTime>(type: "datetime2", nullable: true),
                    reportado_por = table.Column<long>(type: "bigint", nullable: true),
                    actualizado_por = table.Column<long>(type: "bigint", nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_incident", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "landing_section",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    scope = table.Column<string>(type: "nvarchar(10)", maxLength: 10, nullable: false),
                    type = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false),
                    titulo = table.Column<string>(type: "nvarchar(160)", maxLength: 160, nullable: true),
                    subtitulo = table.Column<string>(type: "nvarchar(240)", maxLength: 240, nullable: true),
                    cuerpo = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: true),
                    imagen_url = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: true),
                    cta_label = table.Column<string>(type: "nvarchar(80)", maxLength: 80, nullable: true),
                    cta_url = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: true),
                    orden = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    config_json = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_landing_section", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "lote_qr",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    consecutivo_inicial = table.Column<int>(type: "int", nullable: false),
                    consecutivo_final = table.Column<int>(type: "int", nullable: false),
                    cantidad = table.Column<int>(type: "int", nullable: false),
                    descripcion = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: false),
                    qr_size_cm = table.Column<decimal>(type: "decimal(5,2)", precision: 5, scale: 2, nullable: false),
                    padding_cm = table.Column<decimal>(type: "decimal(5,2)", precision: 5, scale: 2, nullable: false),
                    label_position = table.Column<string>(type: "nvarchar(10)", maxLength: 10, nullable: false),
                    mostrar_bordes = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_lote_qr", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "officers",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    nombre = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: false),
                    apellido_paterno = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: false),
                    apellido_materno = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: true),
                    placa = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    placa_normalizada = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    curp = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: true),
                    curp_idx = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: true),
                    rfc = table.Column<string>(type: "nvarchar(512)", maxLength: 512, nullable: true),
                    rfc_idx = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: true),
                    sexo_id = table.Column<long>(type: "bigint", nullable: false),
                    tipo_sangre_id = table.Column<long>(type: "bigint", nullable: true),
                    municipio = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    estado = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    foto_url = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_officers", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "transfer",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    origen_id = table.Column<long>(type: "bigint", nullable: false),
                    destino_id = table.Column<long>(type: "bigint", nullable: false),
                    status = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false, defaultValue: "ENVIADO"),
                    fecha_envio = table.Column<DateTime>(type: "datetime2", nullable: false),
                    fecha_recepcion = table.Column<DateTime>(type: "datetime2", nullable: true),
                    creado_por = table.Column<long>(type: "bigint", nullable: true),
                    recibido_por = table.Column<long>(type: "bigint", nullable: true),
                    observaciones = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_transfer", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "users",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    name = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    email = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: false),
                    password = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: false),
                    role = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false, defaultValue: "CAPTURISTA"),
                    enabled = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    failed_attempts = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    locked_until = table.Column<DateTime>(type: "datetime2", nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_users", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "warehouse",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    codigo = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    nombre = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    nombre_normalizado = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    tipo_item_id = table.Column<long>(type: "bigint", nullable: false),
                    municipio = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    estado = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    direccion = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: true),
                    cp = table.Column<string>(type: "nvarchar(10)", maxLength: 10, nullable: true),
                    latitud = table.Column<decimal>(type: "decimal(9,6)", precision: 9, scale: 6, nullable: true),
                    longitud = table.Column<decimal>(type: "decimal(9,6)", precision: 9, scale: 6, nullable: true),
                    responsable_id = table.Column<long>(type: "bigint", nullable: true),
                    telefono = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: true),
                    email_contacto = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: true),
                    capacidad = table.Column<int>(type: "int", nullable: true),
                    observaciones = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_warehouse", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "catalog_item",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    catalog_id = table.Column<long>(type: "bigint", nullable: false),
                    code = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: true),
                    nombre = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    nombre_normalizado = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    descripcion = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    foto_url = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    parent_item_id = table.Column<long>(type: "bigint", nullable: true),
                    orden = table.Column<int>(type: "int", nullable: true),
                    active = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_catalog_item", x => x.id);
                    table.ForeignKey(
                        name: "FK_catalog_item_catalog_catalog_id",
                        column: x => x.catalog_id,
                        principalTable: "catalog",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_catalog_item_catalog_item_parent_item_id",
                        column: x => x.parent_item_id,
                        principalTable: "catalog_item",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "transfer_item",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    transfer_id = table.Column<long>(type: "bigint", nullable: false),
                    equipment_id = table.Column<long>(type: "bigint", nullable: false),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_transfer_item", x => x.id);
                    table.ForeignKey(
                        name: "FK_transfer_item_transfer_transfer_id",
                        column: x => x.transfer_id,
                        principalTable: "transfer",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "password_reset_tokens",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    code = table.Column<string>(type: "nvarchar(6)", maxLength: 6, nullable: false),
                    user_id = table.Column<long>(type: "bigint", nullable: false),
                    expires_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_password_reset_tokens", x => x.id);
                    table.ForeignKey(
                        name: "FK_password_reset_tokens_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "verification_tokens",
                columns: table => new
                {
                    id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    code = table.Column<string>(type: "nvarchar(6)", maxLength: 6, nullable: false),
                    user_id = table.Column<long>(type: "bigint", nullable: false),
                    expires_at = table.Column<DateTime>(type: "datetime2", nullable: false),
                    created_at = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_verification_tokens", x => x.id);
                    table.ForeignKey(
                        name: "FK_verification_tokens_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "idx_assignment_fecha",
                table: "assignment",
                column: "fecha_asignacion");

            migrationBuilder.CreateIndex(
                name: "idx_assignment_officer",
                table: "assignment",
                column: "officer_id");

            migrationBuilder.CreateIndex(
                name: "ux_assignment_vigente",
                table: "assignment",
                column: "equipment_id",
                unique: true,
                filter: "[fecha_devolucion] IS NULL");

            migrationBuilder.CreateIndex(
                name: "idx_audit_accion",
                table: "audit_log",
                column: "accion");

            migrationBuilder.CreateIndex(
                name: "idx_audit_entidad",
                table: "audit_log",
                columns: new[] { "entidad", "entidad_id" });

            migrationBuilder.CreateIndex(
                name: "idx_audit_occurred_at",
                table: "audit_log",
                column: "occurred_at");

            migrationBuilder.CreateIndex(
                name: "idx_audit_usuario",
                table: "audit_log",
                column: "usuario_id");

            migrationBuilder.CreateIndex(
                name: "uk_catalog_code",
                table: "catalog",
                column: "code",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "idx_catalog_item_active",
                table: "catalog_item",
                column: "active");

            migrationBuilder.CreateIndex(
                name: "idx_catalog_item_catalog",
                table: "catalog_item",
                column: "catalog_id");

            migrationBuilder.CreateIndex(
                name: "idx_catalog_item_parent",
                table: "catalog_item",
                column: "parent_item_id");

            migrationBuilder.CreateIndex(
                name: "uk_catalog_item_named",
                table: "catalog_item",
                columns: new[] { "catalog_id", "nombre_normalizado" },
                unique: true,
                filter: "[parent_item_id] IS NULL");

            migrationBuilder.CreateIndex(
                name: "uk_catalog_item_named_child",
                table: "catalog_item",
                columns: new[] { "catalog_id", "parent_item_id", "nombre_normalizado" },
                unique: true,
                filter: "[parent_item_id] IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "idx_decommission_equipment",
                table: "decommission",
                column: "equipment_id");

            migrationBuilder.CreateIndex(
                name: "idx_decommission_fecha",
                table: "decommission",
                column: "fecha");

            migrationBuilder.CreateIndex(
                name: "idx_decommission_motivo",
                table: "decommission",
                column: "motivo_id");

            migrationBuilder.CreateIndex(
                name: "uq_decommission_reason_nombre",
                table: "decommission_reason",
                column: "nombre",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "idx_equipment_status",
                table: "equipment",
                column: "status");

            migrationBuilder.CreateIndex(
                name: "idx_equipment_type",
                table: "equipment",
                column: "equipment_type_id");

            migrationBuilder.CreateIndex(
                name: "idx_equipment_vencimiento",
                table: "equipment",
                column: "fecha_vencimiento");

            migrationBuilder.CreateIndex(
                name: "idx_equipment_warehouse",
                table: "equipment",
                column: "warehouse_id");

            migrationBuilder.CreateIndex(
                name: "uk_equipment_codigo_norm",
                table: "equipment",
                column: "codigo_normalizado",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "idx_incident_equipment",
                table: "incident",
                column: "equipment_id");

            migrationBuilder.CreateIndex(
                name: "idx_incident_estado",
                table: "incident",
                column: "estado");

            migrationBuilder.CreateIndex(
                name: "idx_landing_scope_active_orden",
                table: "landing_section",
                columns: new[] { "scope", "active", "orden" });

            migrationBuilder.CreateIndex(
                name: "idx_officers_active",
                table: "officers",
                column: "active");

            migrationBuilder.CreateIndex(
                name: "idx_officers_sexo",
                table: "officers",
                column: "sexo_id");

            migrationBuilder.CreateIndex(
                name: "uk_officers_curp_idx",
                table: "officers",
                column: "curp_idx",
                unique: true,
                filter: "[curp_idx] IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "uk_officers_placa_norm",
                table: "officers",
                column: "placa_normalizada",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uk_officers_rfc_idx",
                table: "officers",
                column: "rfc_idx",
                unique: true,
                filter: "[rfc_idx] IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "uk_password_reset_tokens_code",
                table: "password_reset_tokens",
                column: "code",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uk_password_reset_tokens_user",
                table: "password_reset_tokens",
                column: "user_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "idx_transfer_destino",
                table: "transfer",
                column: "destino_id");

            migrationBuilder.CreateIndex(
                name: "idx_transfer_origen",
                table: "transfer",
                column: "origen_id");

            migrationBuilder.CreateIndex(
                name: "idx_transfer_status",
                table: "transfer",
                column: "status");

            migrationBuilder.CreateIndex(
                name: "idx_transfer_item_equipment",
                table: "transfer_item",
                column: "equipment_id");

            migrationBuilder.CreateIndex(
                name: "idx_transfer_item_transfer",
                table: "transfer_item",
                column: "transfer_id");

            migrationBuilder.CreateIndex(
                name: "idx_users_email",
                table: "users",
                column: "email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uk_verification_tokens_code",
                table: "verification_tokens",
                column: "code",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uk_verification_tokens_user",
                table: "verification_tokens",
                column: "user_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "idx_warehouse_active",
                table: "warehouse",
                column: "active");

            migrationBuilder.CreateIndex(
                name: "idx_warehouse_tipo",
                table: "warehouse",
                column: "tipo_item_id");

            migrationBuilder.CreateIndex(
                name: "uk_warehouse_codigo",
                table: "warehouse",
                column: "codigo",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uk_warehouse_nombre_norm",
                table: "warehouse",
                column: "nombre_normalizado",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "assignment");

            migrationBuilder.DropTable(
                name: "audit_log");

            migrationBuilder.DropTable(
                name: "catalog_item");

            migrationBuilder.DropTable(
                name: "decommission");

            migrationBuilder.DropTable(
                name: "decommission_reason");

            migrationBuilder.DropTable(
                name: "equipment");

            migrationBuilder.DropTable(
                name: "incident");

            migrationBuilder.DropTable(
                name: "landing_section");

            migrationBuilder.DropTable(
                name: "lote_qr");

            migrationBuilder.DropTable(
                name: "officers");

            migrationBuilder.DropTable(
                name: "password_reset_tokens");

            migrationBuilder.DropTable(
                name: "transfer_item");

            migrationBuilder.DropTable(
                name: "verification_tokens");

            migrationBuilder.DropTable(
                name: "warehouse");

            migrationBuilder.DropTable(
                name: "catalog");

            migrationBuilder.DropTable(
                name: "transfer");

            migrationBuilder.DropTable(
                name: "users");
        }
    }
}
