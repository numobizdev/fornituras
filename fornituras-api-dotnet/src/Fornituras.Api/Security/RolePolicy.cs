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
    /// <summary>Generación, consulta y exportación de lotes QR (rol dedicado, ADR 0021).</summary>
    public const string ManageQrLotes = "SUPER_ADMIN";

    private static readonly HashSet<string> FullPiiRoles =
        new(StringComparer.OrdinalIgnoreCase) { "ADMIN", "SUPERVISOR", "AUDITOR" };

    // Captura de foto de elemento (PII): mismos roles que escriben el padrón (WriteOfficers).
    private static readonly HashSet<string> OfficerPhotoWriteRoles =
        new(StringComparer.OrdinalIgnoreCase) { "ADMIN", "SUPERVISOR", "CAPTURISTA" };

    public static bool CanViewFullPii(ClaimsPrincipal? user) => HasAnyRole(user, FullPiiRoles);

    /// <summary>¿El actor puede capturar/subir la foto de un elemento (PII)?</summary>
    public static bool CanCaptureOfficerPhoto(ClaimsPrincipal? user) =>
        HasAnyRole(user, OfficerPhotoWriteRoles);

    /// <summary>¿El actor puede ver sin enmascarar la foto de un elemento (PII)?</summary>
    public static bool CanViewOfficerPhoto(ClaimsPrincipal? user) => HasAnyRole(user, FullPiiRoles);

    private static bool HasAnyRole(ClaimsPrincipal? user, HashSet<string> roles) =>
        user?.Claims
            .Where(c => c.Type == ClaimTypes.Role)
            .Any(c => roles.Contains(c.Value)) == true;
}
