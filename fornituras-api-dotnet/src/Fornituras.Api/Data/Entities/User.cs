namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Usuario de la aplicación con credenciales y rol RBAC.
/// </summary>
public class User : BaseEntity
{
    public string Name { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string Password { get; set; } = string.Empty;

    public Role Role { get; set; } = Role.CAPTURISTA;

    public bool Enabled { get; set; } = true;

    /// <summary>Intentos de login fallidos consecutivos; se reinicia al autenticar con éxito.</summary>
    public int FailedAttempts { get; set; }

    /// <summary>Momento hasta el que la cuenta queda bloqueada; <c>null</c> = sin bloqueo.</summary>
    public DateTime? LockedUntil { get; set; }
}
