using Fornituras.Api.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Data;

public class ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<VerificationToken> VerificationTokens => Set<VerificationToken>();
    public DbSet<PasswordResetToken> PasswordResetTokens => Set<PasswordResetToken>();
    public DbSet<Catalog> Catalogs => Set<Catalog>();
    public DbSet<CatalogItem> CatalogItems => Set<CatalogItem>();
    public DbSet<Warehouse> Warehouses => Set<Warehouse>();
    public DbSet<Equipment> Equipment => Set<Equipment>();
    public DbSet<Officer> Officers => Set<Officer>();
    public DbSet<Assignment> Assignments => Set<Assignment>();
    public DbSet<LoteQr> LoteQrs => Set<LoteQr>();
    public DbSet<Transfer> Transfers => Set<Transfer>();
    public DbSet<TransferItem> TransferItems => Set<TransferItem>();
    public DbSet<Incident> Incidents => Set<Incident>();
    public DbSet<Decommission> Decommissions => Set<Decommission>();
    public DbSet<DecommissionReason> DecommissionReasons => Set<DecommissionReason>();
    public DbSet<AuditLog> AuditLogs => Set<AuditLog>();
    public DbSet<LandingSection> LandingSections => Set<LandingSection>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        ConfigureBaseEntityTimestamps(modelBuilder);
        ConfigureUser(modelBuilder);
        ConfigureVerificationToken(modelBuilder);
        ConfigurePasswordResetToken(modelBuilder);
        ConfigureCatalog(modelBuilder);
        ConfigureCatalogItem(modelBuilder);
        ConfigureWarehouse(modelBuilder);
        ConfigureEquipment(modelBuilder);
        ConfigureOfficer(modelBuilder);
        ConfigureAssignment(modelBuilder);
        ConfigureLoteQr(modelBuilder);
        ConfigureTransfer(modelBuilder);
        ConfigureTransferItem(modelBuilder);
        ConfigureIncident(modelBuilder);
        ConfigureDecommissionReason(modelBuilder);
        ConfigureDecommission(modelBuilder);
        ConfigureAuditLog(modelBuilder);
        ConfigureLandingSection(modelBuilder);
    }

    private static void ConfigureBaseEntityTimestamps(ModelBuilder modelBuilder)
    {
        foreach (var entityType in modelBuilder.Model.GetEntityTypes())
        {
            if (!typeof(BaseEntity).IsAssignableFrom(entityType.ClrType))
            {
                continue;
            }

            modelBuilder.Entity(entityType.ClrType, builder =>
            {
                builder.Property(nameof(BaseEntity.Id)).HasColumnName("id");
                builder.Property(nameof(BaseEntity.CreatedAt)).HasColumnName("created_at");
                builder.Property(nameof(BaseEntity.UpdatedAt)).HasColumnName("updated_at");
            });
        }
    }

    private static void ConfigureUser(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("users");
            entity.Property(e => e.Name).HasColumnName("name").HasMaxLength(100).IsRequired();
            entity.Property(e => e.Email).HasColumnName("email").HasMaxLength(255).IsRequired();
            entity.Property(e => e.Password).HasColumnName("password").HasMaxLength(255).IsRequired();
            entity.Property(e => e.Role).HasColumnName("role").HasMaxLength(20)
                .HasConversion<string>().HasDefaultValue(Role.CAPTURISTA);
            entity.Property(e => e.Enabled).HasColumnName("enabled").HasDefaultValue(true);
            entity.Property(e => e.FailedAttempts).HasColumnName("failed_attempts").HasDefaultValue(0);
            entity.Property(e => e.LockedUntil).HasColumnName("locked_until");

            entity.HasIndex(e => e.Email).IsUnique().HasDatabaseName("uk_users_email");
            entity.HasIndex(e => e.Email).HasDatabaseName("idx_users_email");
        });
    }

    private static void ConfigureVerificationToken(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<VerificationToken>(entity =>
        {
            entity.ToTable("verification_tokens");
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.Code).HasColumnName("code").HasMaxLength(6).IsRequired();
            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.ExpiresAt).HasColumnName("expires_at");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");

            entity.HasOne(e => e.User)
                .WithMany()
                .HasForeignKey(e => e.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => e.Code).IsUnique().HasDatabaseName("uk_verification_tokens_code");
            entity.HasIndex(e => e.UserId).IsUnique().HasDatabaseName("uk_verification_tokens_user");
        });
    }

    private static void ConfigurePasswordResetToken(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PasswordResetToken>(entity =>
        {
            entity.ToTable("password_reset_tokens");
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.Code).HasColumnName("code").HasMaxLength(6).IsRequired();
            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.ExpiresAt).HasColumnName("expires_at");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");

            entity.HasOne(e => e.User)
                .WithMany()
                .HasForeignKey(e => e.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => e.Code).IsUnique().HasDatabaseName("uk_password_reset_tokens_code");
            entity.HasIndex(e => e.UserId).IsUnique().HasDatabaseName("uk_password_reset_tokens_user");
        });
    }

    private static void ConfigureCatalog(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Catalog>(entity =>
        {
            entity.ToTable("catalog");
            entity.Property(e => e.Code).HasColumnName("code").HasMaxLength(40).IsRequired();
            entity.Property(e => e.Nombre).HasColumnName("nombre").HasMaxLength(120).IsRequired();
            entity.Property(e => e.Descripcion).HasColumnName("descripcion").HasMaxLength(500);
            entity.Property(e => e.IsSystem).HasColumnName("is_system").HasDefaultValue(false);
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);

            entity.HasIndex(e => e.Code).IsUnique().HasDatabaseName("uk_catalog_code");
        });
    }

    private static void ConfigureCatalogItem(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<CatalogItem>(entity =>
        {
            entity.ToTable("catalog_item");
            entity.Property(e => e.CatalogId).HasColumnName("catalog_id");
            entity.Property(e => e.Code).HasColumnName("code").HasMaxLength(40);
            entity.Property(e => e.Nombre).HasColumnName("nombre").HasMaxLength(120).IsRequired();
            entity.Property(e => e.NombreNormalizado).HasColumnName("nombre_normalizado").HasMaxLength(120).IsRequired();
            entity.Property(e => e.Descripcion).HasColumnName("descripcion").HasMaxLength(500);
            entity.Property(e => e.FotoUrl).HasColumnName("foto_url").HasMaxLength(500);
            entity.Property(e => e.ParentItemId).HasColumnName("parent_item_id");
            entity.Property(e => e.Orden).HasColumnName("orden");
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);

            entity.HasOne(e => e.Catalog)
                .WithMany(c => c.Items)
                .HasForeignKey(e => e.CatalogId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(e => e.ParentItem)
                .WithMany(p => p.ChildItems)
                .HasForeignKey(e => e.ParentItemId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasIndex(e => e.CatalogId).HasDatabaseName("idx_catalog_item_catalog");
            entity.HasIndex(e => e.Active).HasDatabaseName("idx_catalog_item_active");
            entity.HasIndex(e => e.ParentItemId).HasDatabaseName("idx_catalog_item_parent");

            entity.HasIndex(e => new { e.CatalogId, e.NombreNormalizado })
                .IsUnique()
                .HasFilter("[parent_item_id] IS NULL")
                .HasDatabaseName("uk_catalog_item_named");

            entity.HasIndex(e => new { e.CatalogId, e.ParentItemId, e.NombreNormalizado })
                .IsUnique()
                .HasFilter("[parent_item_id] IS NOT NULL")
                .HasDatabaseName("uk_catalog_item_named_child");
        });
    }

    private static void ConfigureWarehouse(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Warehouse>(entity =>
        {
            entity.ToTable("warehouse");
            entity.Property(e => e.Codigo).HasColumnName("codigo").HasMaxLength(40).IsRequired();
            entity.Property(e => e.Nombre).HasColumnName("nombre").HasMaxLength(120).IsRequired();
            entity.Property(e => e.NombreNormalizado).HasColumnName("nombre_normalizado").HasMaxLength(120).IsRequired();
            entity.Property(e => e.TipoItemId).HasColumnName("tipo_item_id");
            entity.Property(e => e.Municipio).HasColumnName("municipio").HasMaxLength(120);
            entity.Property(e => e.Estado).HasColumnName("estado").HasMaxLength(120);
            entity.Property(e => e.Direccion).HasColumnName("direccion").HasMaxLength(255);
            entity.Property(e => e.Cp).HasColumnName("cp").HasMaxLength(10);
            entity.Property(e => e.Latitud).HasColumnName("latitud").HasPrecision(9, 6);
            entity.Property(e => e.Longitud).HasColumnName("longitud").HasPrecision(9, 6);
            entity.Property(e => e.ResponsableId).HasColumnName("responsable_id");
            entity.Property(e => e.Telefono).HasColumnName("telefono").HasMaxLength(30);
            entity.Property(e => e.EmailContacto).HasColumnName("email_contacto").HasMaxLength(255);
            entity.Property(e => e.Capacidad).HasColumnName("capacidad");
            entity.Property(e => e.Observaciones).HasColumnName("observaciones").HasMaxLength(500);
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);

            entity.HasIndex(e => e.Codigo).IsUnique().HasDatabaseName("uk_warehouse_codigo");
            entity.HasIndex(e => e.NombreNormalizado).IsUnique().HasDatabaseName("uk_warehouse_nombre_norm");
            entity.HasIndex(e => e.Active).HasDatabaseName("idx_warehouse_active");
            entity.HasIndex(e => e.TipoItemId).HasDatabaseName("idx_warehouse_tipo");
        });
    }

    private static void ConfigureEquipment(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Equipment>(entity =>
        {
            entity.ToTable("equipment");
            entity.Property(e => e.CodigoQr).HasColumnName("codigo_qr").HasMaxLength(60).IsRequired();
            entity.Property(e => e.CodigoNormalizado).HasColumnName("codigo_normalizado").HasMaxLength(60).IsRequired();
            entity.Property(e => e.EquipmentTypeId).HasColumnName("equipment_type_id");
            entity.Property(e => e.SizeId).HasColumnName("size_id");
            entity.Property(e => e.WarehouseId).HasColumnName("warehouse_id");
            entity.Property(e => e.Status).HasColumnName("status").HasMaxLength(20)
                .HasConversion<string>().HasDefaultValue(EquipmentStatus.DISPONIBLE);
            entity.Property(e => e.Descripcion).HasColumnName("descripcion").HasMaxLength(255);
            entity.Property(e => e.Marca).HasColumnName("marca").HasMaxLength(120);
            entity.Property(e => e.Modelo).HasColumnName("modelo").HasMaxLength(120);
            entity.Property(e => e.NivelBalistico).HasColumnName("nivel_balistico").HasMaxLength(60);
            entity.Property(e => e.NumeroInventario).HasColumnName("numero_inventario").HasMaxLength(60);
            entity.Property(e => e.FechaFabricacion).HasColumnName("fecha_fabricacion");
            entity.Property(e => e.FechaAdquisicion).HasColumnName("fecha_adquisicion");
            entity.Property(e => e.VidaUtilMeses).HasColumnName("vida_util_meses");
            entity.Property(e => e.FechaVencimiento).HasColumnName("fecha_vencimiento");
            entity.Property(e => e.Observaciones).HasColumnName("observaciones").HasMaxLength(500);
            entity.Property(e => e.FotoUrl).HasColumnName("foto_url").HasMaxLength(500);

            entity.HasIndex(e => e.CodigoNormalizado).IsUnique().HasDatabaseName("uk_equipment_codigo_norm");
            entity.HasIndex(e => e.Status).HasDatabaseName("idx_equipment_status");
            entity.HasIndex(e => e.EquipmentTypeId).HasDatabaseName("idx_equipment_type");
            entity.HasIndex(e => e.WarehouseId).HasDatabaseName("idx_equipment_warehouse");
            entity.HasIndex(e => e.FechaVencimiento).HasDatabaseName("idx_equipment_vencimiento");
        });
    }

    private static void ConfigureOfficer(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Officer>(entity =>
        {
            entity.ToTable("officers");
            entity.Property(e => e.Nombre).HasColumnName("nombre").HasMaxLength(512).IsRequired();
            entity.Property(e => e.ApellidoPaterno).HasColumnName("apellido_paterno").HasMaxLength(512).IsRequired();
            entity.Property(e => e.ApellidoMaterno).HasColumnName("apellido_materno").HasMaxLength(512);
            entity.Property(e => e.Placa).HasColumnName("placa").HasMaxLength(40).IsRequired();
            entity.Property(e => e.PlacaNormalizada).HasColumnName("placa_normalizada").HasMaxLength(40).IsRequired();
            entity.Property(e => e.Curp).HasColumnName("curp").HasMaxLength(512);
            entity.Property(e => e.CurpIdx).HasColumnName("curp_idx").HasMaxLength(64);
            entity.Property(e => e.Rfc).HasColumnName("rfc").HasMaxLength(512);
            entity.Property(e => e.RfcIdx).HasColumnName("rfc_idx").HasMaxLength(64);
            entity.Property(e => e.SexoId).HasColumnName("sexo_id");
            entity.Property(e => e.TipoSangreId).HasColumnName("tipo_sangre_id");
            entity.Property(e => e.Municipio).HasColumnName("municipio").HasMaxLength(120);
            entity.Property(e => e.Estado).HasColumnName("estado").HasMaxLength(120);
            entity.Property(e => e.FotoUrl).HasColumnName("foto_url").HasMaxLength(500);
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);

            entity.HasIndex(e => e.PlacaNormalizada).IsUnique().HasDatabaseName("uk_officers_placa_norm");
            entity.HasIndex(e => e.CurpIdx).IsUnique()
                .HasFilter("[curp_idx] IS NOT NULL")
                .HasDatabaseName("uk_officers_curp_idx");
            entity.HasIndex(e => e.RfcIdx).IsUnique()
                .HasFilter("[rfc_idx] IS NOT NULL")
                .HasDatabaseName("uk_officers_rfc_idx");
            entity.HasIndex(e => e.SexoId).HasDatabaseName("idx_officers_sexo");
            entity.HasIndex(e => e.Active).HasDatabaseName("idx_officers_active");
        });
    }

    private static void ConfigureAssignment(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Assignment>(entity =>
        {
            entity.ToTable("assignment");
            entity.Property(e => e.EquipmentId).HasColumnName("equipment_id");
            entity.Property(e => e.OfficerId).HasColumnName("officer_id");
            entity.Property(e => e.FechaAsignacion).HasColumnName("fecha_asignacion");
            entity.Property(e => e.FechaDevolucion).HasColumnName("fecha_devolucion");
            entity.Property(e => e.AsignadoPor).HasColumnName("asignado_por");
            entity.Property(e => e.RecibidoPor).HasColumnName("recibido_por");
            entity.Property(e => e.FirmaUrl).HasColumnName("firma_url").HasMaxLength(500);
            entity.Property(e => e.Observaciones).HasColumnName("observaciones").HasMaxLength(500);

            entity.HasIndex(e => e.EquipmentId)
                .IsUnique()
                .HasFilter("[fecha_devolucion] IS NULL")
                .HasDatabaseName("ux_assignment_vigente");

            entity.HasIndex(e => e.OfficerId).HasDatabaseName("idx_assignment_officer");
            entity.HasIndex(e => e.FechaAsignacion).HasDatabaseName("idx_assignment_fecha");
        });
    }

    private static void ConfigureLoteQr(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<LoteQr>(entity =>
        {
            entity.ToTable("lote_qr");
            entity.Property(e => e.ConsecutivoInicial).HasColumnName("consecutivo_inicial");
            entity.Property(e => e.ConsecutivoFinal).HasColumnName("consecutivo_final");
            entity.Property(e => e.Cantidad).HasColumnName("cantidad");
            entity.Property(e => e.Descripcion).HasColumnName("descripcion").HasMaxLength(255).IsRequired();
            entity.Property(e => e.QrSizeCm).HasColumnName("qr_size_cm").HasPrecision(5, 2);
            entity.Property(e => e.PaddingCm).HasColumnName("padding_cm").HasPrecision(5, 2);
            entity.Property(e => e.LabelPosition).HasColumnName("label_position").HasMaxLength(10)
                .HasConversion<string>();
            entity.Property(e => e.MostrarBordes).HasColumnName("mostrar_bordes").HasDefaultValue(true);
        });
    }

    private static void ConfigureTransfer(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Transfer>(entity =>
        {
            entity.ToTable("transfer");
            entity.Property(e => e.OrigenId).HasColumnName("origen_id");
            entity.Property(e => e.DestinoId).HasColumnName("destino_id");
            entity.Property(e => e.Status).HasColumnName("status").HasMaxLength(20)
                .HasConversion<string>().HasDefaultValue(TransferStatus.ENVIADO);
            entity.Property(e => e.FechaEnvio).HasColumnName("fecha_envio");
            entity.Property(e => e.FechaRecepcion).HasColumnName("fecha_recepcion");
            entity.Property(e => e.CreadoPor).HasColumnName("creado_por");
            entity.Property(e => e.RecibidoPor).HasColumnName("recibido_por");
            entity.Property(e => e.Observaciones).HasColumnName("observaciones").HasMaxLength(500);

            entity.HasIndex(e => e.Status).HasDatabaseName("idx_transfer_status");
            entity.HasIndex(e => e.OrigenId).HasDatabaseName("idx_transfer_origen");
            entity.HasIndex(e => e.DestinoId).HasDatabaseName("idx_transfer_destino");
        });
    }

    private static void ConfigureTransferItem(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TransferItem>(entity =>
        {
            entity.ToTable("transfer_item");
            entity.Property(e => e.TransferId).HasColumnName("transfer_id");
            entity.Property(e => e.EquipmentId).HasColumnName("equipment_id");

            entity.HasOne(e => e.Transfer)
                .WithMany(t => t.Items)
                .HasForeignKey(e => e.TransferId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => e.TransferId).HasDatabaseName("idx_transfer_item_transfer");
            entity.HasIndex(e => e.EquipmentId).HasDatabaseName("idx_transfer_item_equipment");
        });
    }

    private static void ConfigureIncident(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Incident>(entity =>
        {
            entity.ToTable("incident");
            entity.Property(e => e.EquipmentId).HasColumnName("equipment_id");
            entity.Property(e => e.Tipo).HasColumnName("tipo").HasMaxLength(20).HasConversion<string>();
            entity.Property(e => e.Descripcion).HasColumnName("descripcion").HasMaxLength(500).IsRequired();
            entity.Property(e => e.Estado).HasColumnName("estado").HasMaxLength(20)
                .HasConversion<string>().HasDefaultValue(IncidentStatus.ABIERTA);
            entity.Property(e => e.FechaReporte).HasColumnName("fecha_reporte");
            entity.Property(e => e.FechaResolucion).HasColumnName("fecha_resolucion");
            entity.Property(e => e.ReportadoPor).HasColumnName("reportado_por");
            entity.Property(e => e.ActualizadoPor).HasColumnName("actualizado_por");

            entity.HasIndex(e => e.Estado).HasDatabaseName("idx_incident_estado");
            entity.HasIndex(e => e.EquipmentId).HasDatabaseName("idx_incident_equipment");
        });
    }

    private static void ConfigureDecommissionReason(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<DecommissionReason>(entity =>
        {
            entity.ToTable("decommission_reason");
            entity.Property(e => e.Nombre).HasColumnName("nombre").HasMaxLength(100).IsRequired();
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);

            entity.HasIndex(e => e.Nombre).IsUnique().HasDatabaseName("uq_decommission_reason_nombre");
        });
    }

    private static void ConfigureDecommission(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Decommission>(entity =>
        {
            entity.ToTable("decommission");
            entity.Property(e => e.EquipmentId).HasColumnName("equipment_id");
            entity.Property(e => e.MotivoId).HasColumnName("motivo_id");
            entity.Property(e => e.Fecha).HasColumnName("fecha");
            entity.Property(e => e.Responsable).HasColumnName("responsable");
            entity.Property(e => e.Observaciones).HasColumnName("observaciones").HasMaxLength(500);

            entity.HasIndex(e => e.Fecha).HasDatabaseName("idx_decommission_fecha");
            entity.HasIndex(e => e.MotivoId).HasDatabaseName("idx_decommission_motivo");
            entity.HasIndex(e => e.EquipmentId).HasDatabaseName("idx_decommission_equipment");
        });
    }

    private static void ConfigureAuditLog(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AuditLog>(entity =>
        {
            entity.ToTable("audit_log");
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.UsuarioId).HasColumnName("usuario_id");
            entity.Property(e => e.Actor).HasColumnName("actor").HasMaxLength(150);
            entity.Property(e => e.Accion).HasColumnName("accion").HasMaxLength(80).IsRequired();
            entity.Property(e => e.Entidad).HasColumnName("entidad").HasMaxLength(60);
            entity.Property(e => e.EntidadId).HasColumnName("entidad_id");
            entity.Property(e => e.OccurredAt).HasColumnName("occurred_at");
            entity.Property(e => e.Ip).HasColumnName("ip").HasMaxLength(45);
            entity.Property(e => e.Evidencia).HasColumnName("evidencia").HasMaxLength(1000);
            entity.Property(e => e.PrevHash).HasColumnName("prev_hash").HasMaxLength(64);

            entity.HasIndex(e => e.UsuarioId).HasDatabaseName("idx_audit_usuario");
            entity.HasIndex(e => e.Accion).HasDatabaseName("idx_audit_accion");
            entity.HasIndex(e => new { e.Entidad, e.EntidadId }).HasDatabaseName("idx_audit_entidad");
            entity.HasIndex(e => e.OccurredAt).HasDatabaseName("idx_audit_occurred_at");
        });
    }

    private static void ConfigureLandingSection(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<LandingSection>(entity =>
        {
            entity.ToTable("landing_section");
            entity.Property(e => e.Scope).HasColumnName("scope").HasMaxLength(10).HasConversion<string>();
            entity.Property(e => e.Type).HasColumnName("type").HasMaxLength(20).HasConversion<string>();
            entity.Property(e => e.Titulo).HasColumnName("titulo").HasMaxLength(160);
            entity.Property(e => e.Subtitulo).HasColumnName("subtitulo").HasMaxLength(240);
            entity.Property(e => e.Cuerpo).HasColumnName("cuerpo").HasMaxLength(2000);
            entity.Property(e => e.ImagenUrl).HasColumnName("imagen_url").HasMaxLength(512);
            entity.Property(e => e.CtaLabel).HasColumnName("cta_label").HasMaxLength(80);
            entity.Property(e => e.CtaUrl).HasColumnName("cta_url").HasMaxLength(512);
            entity.Property(e => e.Orden).HasColumnName("orden").HasDefaultValue(0);
            entity.Property(e => e.Active).HasColumnName("active").HasDefaultValue(true);
            entity.Property(e => e.ConfigJson).HasColumnName("config_json");

            entity.HasIndex(e => new { e.Scope, e.Active, e.Orden })
                .HasDatabaseName("idx_landing_scope_active_orden");
        });
    }
}
