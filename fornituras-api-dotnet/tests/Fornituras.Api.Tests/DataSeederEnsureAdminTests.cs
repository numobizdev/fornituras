using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Tests;

/// <summary>
/// 021 (FR-001): el seeder DEBE garantizar rol ADMIN y cuenta habilitada para el admin
/// configurado, no solo crearlo cuando falta. Causa raíz del reporte "admin sin botones".
/// </summary>
public class DataSeederEnsureAdminTests
{
    private const string AdminEmail = "admin@fornituras.local";

    private static ApplicationDbContext NewDb() =>
        new(new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase($"seed-{Guid.NewGuid():N}")
            .Options);

    private static DataSeeder Seeder(ApplicationDbContext db, bool enabled = true, string email = AdminEmail)
    {
        var options = new AppOptions
        {
            Seed = new SeedOptions
            {
                Admin = new SeedAdminOptions
                {
                    Enabled = enabled,
                    Name = "Administrador",
                    Email = email,
                    Password = "Secreta#2026"
                }
            }
        };

        return new DataSeeder(db, Options.Create(options), NullLogger<DataSeeder>.Instance);
    }

    private static User ExistingAdmin(Role role = Role.ADMIN, bool enabled = true) => new()
    {
        Name = "Administrador",
        Email = AdminEmail,
        Password = "hash-previo",
        Role = role,
        Enabled = enabled,
        CreatedAt = DateTime.UtcNow.AddDays(-30),
        UpdatedAt = DateTime.UtcNow.AddDays(-30)
    };

    [Fact]
    public async Task Existing_admin_with_lower_role_is_corrected_to_admin()
    {
        using var db = NewDb();
        db.Users.Add(ExistingAdmin(role: Role.CAPTURISTA));
        await db.SaveChangesAsync();

        await Seeder(db).SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.Equal(Role.ADMIN, user.Role);
        Assert.True(user.Enabled);
    }

    [Fact]
    public async Task Existing_admin_disabled_is_re_enabled()
    {
        using var db = NewDb();
        db.Users.Add(ExistingAdmin(enabled: false));
        await db.SaveChangesAsync();

        await Seeder(db).SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.True(user.Enabled);
        Assert.Equal(Role.ADMIN, user.Role);
    }

    [Fact]
    public async Task Correct_admin_is_left_untouched()
    {
        using var db = NewDb();
        var original = ExistingAdmin();
        db.Users.Add(original);
        await db.SaveChangesAsync();
        var originalUpdatedAt = original.UpdatedAt;

        await Seeder(db).SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.Equal(Role.ADMIN, user.Role);
        Assert.True(user.Enabled);
        Assert.Equal("hash-previo", user.Password);
        Assert.Equal(originalUpdatedAt, user.UpdatedAt);
    }

    [Fact]
    public async Task Missing_admin_is_created_as_admin()
    {
        using var db = NewDb();

        await Seeder(db).SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.Equal(Role.ADMIN, user.Role);
        Assert.True(user.Enabled);
    }

    [Fact]
    public async Task Seed_disabled_changes_nothing()
    {
        using var db = NewDb();
        db.Users.Add(ExistingAdmin(role: Role.CAPTURISTA, enabled: false));
        await db.SaveChangesAsync();

        await Seeder(db, enabled: false).SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.Equal(Role.CAPTURISTA, user.Role);
        Assert.False(user.Enabled);
    }

    [Fact]
    public async Task Empty_email_changes_nothing()
    {
        using var db = NewDb();
        db.Users.Add(ExistingAdmin(role: Role.CAPTURISTA));
        await db.SaveChangesAsync();

        await Seeder(db, email: "  ").SeedAsync();

        var user = await db.Users.SingleAsync(u => u.Email == AdminEmail);
        Assert.Equal(Role.CAPTURISTA, user.Role);
    }

    [Fact]
    public async Task Ensure_never_touches_other_accounts()
    {
        using var db = NewDb();
        db.Users.Add(new User
        {
            Name = "Otro",
            Email = "otro@fornituras.local",
            Password = "hash",
            Role = Role.CAPTURISTA,
            Enabled = false,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        });
        await db.SaveChangesAsync();

        await Seeder(db).SeedAsync();

        var other = await db.Users.SingleAsync(u => u.Email == "otro@fornituras.local");
        Assert.Equal(Role.CAPTURISTA, other.Role);
        Assert.False(other.Enabled);
    }
}
