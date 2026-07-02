using Fornituras.Api.Common;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Data;

/// <summary>
/// Siembra idempotente de datos iniciales (admin, catálogos, motivos de baja, landing).
/// </summary>
public sealed class DataSeeder(
    ApplicationDbContext db,
    IOptions<AppOptions> options,
    ILogger<DataSeeder> logger)
{
    private const string TestUserEmail = "responsable.prueba@fornituras.local";
    private const string TestUserName = "Responsable de Prueba";
    private const string TestUserPassword = "Prueba#2026";

    public async Task SeedAsync(CancellationToken cancellationToken = default)
    {
        await SeedCatalogsAsync(cancellationToken);
        await SeedDecommissionReasonsAsync(cancellationToken);
        await SeedLandingAsync(cancellationToken);

        if (options.Value.Seed.Admin.Enabled)
        {
            await SeedAdminAsync(cancellationToken);
            await SeedTestUserAsync(cancellationToken);
        }
    }

    private async Task SeedAdminAsync(CancellationToken cancellationToken)
    {
        var admin = options.Value.Seed.Admin;
        if (string.IsNullOrWhiteSpace(admin.Email))
        {
            return;
        }

        var existing = await db.Users.SingleOrDefaultAsync(u => u.Email == admin.Email, cancellationToken);
        if (existing is not null)
        {
            // Ensure: la cuenta admin configurada debe quedar siempre ADMIN y habilitada
            // (021, FR-001). Si existía con otro rol/deshabilitada, se corrige y se registra.
            if (existing.Role == Role.ADMIN && existing.Enabled)
            {
                logger.LogDebug("Initial admin user already exists: {Email}", admin.Email);
                return;
            }

            var previousRole = existing.Role;
            var previousEnabled = existing.Enabled;
            existing.Role = Role.ADMIN;
            existing.Enabled = true;
            existing.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(cancellationToken);
            logger.LogWarning(
                "Seed admin corrected: role {PreviousRole} -> ADMIN, enabled {PreviousEnabled} -> true",
                previousRole,
                previousEnabled);
            return;
        }

        var now = DateTime.UtcNow;
        db.Users.Add(new User
        {
            Name = admin.Name,
            Email = admin.Email.Trim(),
            Password = BCrypt.Net.BCrypt.HashPassword(admin.Password),
            Role = Role.ADMIN,
            Enabled = true,
            CreatedAt = now,
            UpdatedAt = now
        });
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Initial admin user seeded: {Email}", admin.Email);
    }

    private async Task SeedTestUserAsync(CancellationToken cancellationToken)
    {
        if (await db.Users.AnyAsync(u => u.Email == TestUserEmail, cancellationToken))
        {
            return;
        }

        var now = DateTime.UtcNow;
        db.Users.Add(new User
        {
            Name = TestUserName,
            Email = TestUserEmail,
            Password = BCrypt.Net.BCrypt.HashPassword(TestUserPassword),
            Role = Role.ALMACEN,
            Enabled = true,
            CreatedAt = now,
            UpdatedAt = now
        });
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Test user seeded (dev): {Email}", TestUserEmail);
    }

    private async Task SeedCatalogsAsync(CancellationToken cancellationToken)
    {
        await EnsureCatalogAsync(
            CatalogCodes.TipoPrenda,
            "Tipo de prenda",
            "Tipo de prenda controlada por el sistema. Hoy: Fornitura.",
            [
                ("Fornitura", "fornitura", "Prenda de dotación controlada.", 1, null)
            ],
            cancellationToken);

        await EnsureCatalogAsync(
            CatalogCodes.Talla,
            "Tallas",
            "Tallas de fornitura, opcionalmente ligadas a un tipo.",
            [],
            cancellationToken);

        await EnsureCatalogAsync(
            CatalogCodes.TipoAlmacen,
            "Tipos de almacén",
            "Clasificación operativa de los almacenes.",
            [
                ("Central", "central", null, 1, "CENTRAL"),
                ("Regional", "regional", null, 2, "REGIONAL"),
                ("Móvil", "movil", null, 3, "MOVIL"),
                ("Temporal", "temporal", null, 4, "TEMPORAL")
            ],
            cancellationToken);

        await EnsureCatalogAsync(
            CatalogCodes.Sexo,
            "Sexo",
            "Sexo del elemento policial.",
            [
                ("Masculino", "masculino", null, 1, null),
                ("Femenino", "femenino", null, 2, null),
                ("No binario", "no binario", null, 3, null),
                ("Prefiero no decir", "prefiero no decir", null, 4, null)
            ],
            cancellationToken);

        await EnsureCatalogAsync(
            CatalogCodes.TipoSangre,
            "Tipo de sangre",
            "Tipo de sangre del elemento policial.",
            [
                ("A+", "a+", null, 1, null),
                ("A-", "a-", null, 2, null),
                ("B+", "b+", null, 3, null),
                ("B-", "b-", null, 4, null),
                ("AB+", "ab+", null, 5, null),
                ("AB-", "ab-", null, 6, null),
                ("O+", "o+", null, 7, null),
                ("O-", "o-", null, 8, null)
            ],
            cancellationToken);
    }

    private async Task EnsureCatalogAsync(
        string code,
        string nombre,
        string descripcion,
        IReadOnlyList<(string Nombre, string Normalizado, string? Desc, int Orden, string? ItemCode)> items,
        CancellationToken cancellationToken)
    {
        var catalog = await db.Catalogs.FirstOrDefaultAsync(c => c.Code == code, cancellationToken);
        var now = DateTime.UtcNow;

        if (catalog is null)
        {
            catalog = new Catalog
            {
                Code = code,
                Nombre = nombre,
                Descripcion = descripcion,
                IsSystem = true,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            };
            db.Catalogs.Add(catalog);
            await db.SaveChangesAsync(cancellationToken);
        }

        foreach (var item in items)
        {
            var exists = await db.CatalogItems.AnyAsync(
                i => i.CatalogId == catalog.Id && i.NombreNormalizado == item.Normalizado,
                cancellationToken);

            if (exists)
            {
                continue;
            }

            db.CatalogItems.Add(new CatalogItem
            {
                CatalogId = catalog.Id,
                Code = item.ItemCode,
                Nombre = item.Nombre,
                NombreNormalizado = item.Normalizado,
                Descripcion = item.Desc,
                Orden = item.Orden,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            });
        }

        await db.SaveChangesAsync(cancellationToken);
    }

    private async Task SeedDecommissionReasonsAsync(CancellationToken cancellationToken)
    {
        var reasons = new[] { "Caducidad", "Daño", "Extravío", "Obsolescencia" };
        var now = DateTime.UtcNow;

        foreach (var nombre in reasons)
        {
            if (await db.DecommissionReasons.AnyAsync(r => r.Nombre == nombre, cancellationToken))
            {
                continue;
            }

            db.DecommissionReasons.Add(new DecommissionReason
            {
                Nombre = nombre,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            });
        }

        await db.SaveChangesAsync(cancellationToken);
    }

    private async Task SeedLandingAsync(CancellationToken cancellationToken)
    {
        if (await db.LandingSections.AnyAsync(s => s.Scope == LandingScope.PUBLIC, cancellationToken))
        {
            return;
        }

        var now = DateTime.UtcNow;
        var sections = new[]
        {
            new LandingSection
            {
                Scope = LandingScope.PUBLIC,
                Type = LandingSectionType.HERO,
                Titulo = "Sistema Integral de Gestión de Fornituras",
                Subtitulo = "Acceso institucional",
                CtaLabel = "Acceder",
                CtaUrl = "/login",
                Orden = 0,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            },
            new LandingSection
            {
                Scope = LandingScope.HOME,
                Type = LandingSectionType.HERO,
                Titulo = "Bienvenido a SIGEFOR",
                Subtitulo = "Panel de gestión de blindajes y dotación",
                Orden = 0,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            },
            new LandingSection
            {
                Scope = LandingScope.HOME,
                Type = LandingSectionType.ANNOUNCEMENT,
                Titulo = "Aviso",
                Cuerpo = "Mantén al día el inventario y las asignaciones para reflejar la disponibilidad real del equipo.",
                Orden = 1,
                Active = true,
                CreatedAt = now,
                UpdatedAt = now
            },
            new LandingSection
            {
                Scope = LandingScope.HOME,
                Type = LandingSectionType.QUICK_LINKS,
                Titulo = "Accesos rápidos",
                Orden = 2,
                Active = true,
                ConfigJson =
                    """[{"label":"Elementos","url":"/elementos","icon":"people-outline"},{"label":"Fornituras","url":"/fornituras","icon":"cube-outline"},{"label":"Asignación","url":"/asignacion","icon":"link-outline"}]""",
                CreatedAt = now,
                UpdatedAt = now
            }
        };

        db.LandingSections.AddRange(sections);
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Default landing sections seeded.");
    }
}
