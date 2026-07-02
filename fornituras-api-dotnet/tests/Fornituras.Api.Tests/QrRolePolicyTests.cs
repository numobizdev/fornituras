using Fornituras.Api.Security;

namespace Fornituras.Api.Tests;

/// <summary>
/// Cobertura de RBAC para lotes QR (ADR 0021, spec 021).
/// </summary>
public class QrRolePolicyTests
{
    [Fact]
    public void ManageQrLotes_is_super_admin_only()
    {
        Assert.Equal("SUPER_ADMIN", RolePolicy.ManageQrLotes);
    }

    [Fact]
    public void WriteInventory_does_not_include_super_admin()
    {
        Assert.DoesNotContain("SUPER_ADMIN", RolePolicy.WriteInventory);
    }

    [Theory]
    [InlineData("ADMIN")]
    [InlineData("ALMACEN")]
    [InlineData("CAPTURISTA")]
    public void Operational_inventory_roles_cannot_manage_qr_lotes(string role)
    {
        Assert.NotEqual(RolePolicy.ManageQrLotes, role);
        Assert.Contains(role, RolePolicy.WriteInventory);
    }
}
