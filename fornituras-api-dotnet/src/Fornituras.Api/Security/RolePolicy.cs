using System.Security.Claims;

namespace Fornituras.Api.Security;

/// <summary>
/// Matriz RBAC centralizada (equivalente a RolePolicy.java, ADR 0013).
/// </summary>
public static class RolePolicy
{
    public const string WriteInventory = "ADMIN,ALMACEN,CAPTURISTA";
    public const string WriteTransfers = "ADMIN,SUPERVISOR,ALMACEN,CAPTURISTA";
    public const string WriteOperations = "ADMIN,SUPERVISOR,CAPTURISTA";
    public const string AuthorizeDecommission = "ADMIN,SUPERVISOR";
    public const string WriteOfficers = "ADMIN,SUPERVISOR,CAPTURISTA";
    public const string ManageConfig = "ADMIN";
    public const string ManageLanding = "ADMIN";
    public const string ManageUsers = "ADMIN";
    public const string ReadAudit = "ADMIN,AUDITOR";

    private static readonly HashSet<string> FullPiiRoles =
        new(StringComparer.OrdinalIgnoreCase) { "ADMIN", "SUPERVISOR", "AUDITOR" };

    public static bool CanViewFullPii(ClaimsPrincipal? user) =>
        user?.Claims
            .Where(c => c.Type == ClaimTypes.Role)
            .Any(c => FullPiiRoles.Contains(c.Value)) == true;
}
