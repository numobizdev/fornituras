using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace Fornituras.Api.Tests;

/// <summary>
/// Filtro por rol/estado en el listado de usuarios: el selector de responsable de
/// almacén solo debe ofrecer usuarios activos con rol ALMACEN (ADR 0013).
/// </summary>
public class UserServiceFindAllTests
{
    private static ApplicationDbContext NewDb() =>
        new(new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase($"users-{Guid.NewGuid():N}")
            .Options);

    private static UserService Service(ApplicationDbContext db) =>
        new(db, null!, null!, new NoOpAuditWriter(), NullLogger<UserService>.Instance);

    private static User NewUser(string email, Role role, bool enabled = true) => new()
    {
        Name = $"Usuario {email}",
        Email = email,
        Password = "hash",
        Role = role,
        Enabled = enabled,
        CreatedAt = DateTime.UtcNow,
        UpdatedAt = DateTime.UtcNow
    };

    private static async Task<ApplicationDbContext> SeededDbAsync()
    {
        var db = NewDb();
        db.Users.AddRange(
            NewUser("admin@x.mx", Role.ADMIN),
            NewUser("almacen1@x.mx", Role.ALMACEN),
            NewUser("almacen2@x.mx", Role.ALMACEN, enabled: false),
            NewUser("capturista@x.mx", Role.CAPTURISTA));
        await db.SaveChangesAsync();
        return db;
    }

    [Fact]
    public async Task Without_filters_returns_all_users()
    {
        using var db = await SeededDbAsync();

        var page = await Service(db).FindAllAsync(new PaginationQuery());

        Assert.Equal(4, page.TotalElements);
    }

    [Fact]
    public async Task Role_filter_returns_only_that_role()
    {
        using var db = await SeededDbAsync();

        var page = await Service(db).FindAllAsync(new PaginationQuery(), role: "ALMACEN");

        Assert.Equal(2, page.TotalElements);
        Assert.All(page.Content, u => Assert.Equal("ALMACEN", u.Role));
    }

    [Fact]
    public async Task Role_filter_is_case_insensitive()
    {
        using var db = await SeededDbAsync();

        var page = await Service(db).FindAllAsync(new PaginationQuery(), role: "almacen");

        Assert.Equal(2, page.TotalElements);
    }

    [Fact]
    public async Task Invalid_role_filter_is_rejected()
    {
        using var db = await SeededDbAsync();

        await Assert.ThrowsAsync<BadRequestException>(
            () => Service(db).FindAllAsync(new PaginationQuery(), role: "NO_EXISTE"));
    }

    [Fact]
    public async Task Enabled_filter_returns_only_active_users()
    {
        using var db = await SeededDbAsync();

        var page = await Service(db).FindAllAsync(new PaginationQuery(), role: "ALMACEN", enabled: true);

        var user = Assert.Single(page.Content);
        Assert.Equal("almacen1@x.mx", user.Email);
    }

    /// <summary>
    /// Hay usuarios con rol SUPER_ADMIN persistidos en BD (021-ui-lotes-qr); si el enum
    /// dejara de conocerlo, EF lanzaría al materializar y todo listado respondería 500.
    /// </summary>
    [Fact]
    public void Role_enum_accepts_persisted_super_admin()
    {
        Assert.True(Enum.TryParse<Role>("SUPER_ADMIN", out _));
    }
}
