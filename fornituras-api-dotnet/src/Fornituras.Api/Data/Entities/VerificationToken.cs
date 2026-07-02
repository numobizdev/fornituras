namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Código de verificación de cuenta (6 dígitos) asociado a un usuario.
/// </summary>
public class VerificationToken
{
    public long Id { get; set; }

    public string Code { get; set; } = string.Empty;

    public long UserId { get; set; }

    public User User { get; set; } = null!;

    public DateTime ExpiresAt { get; set; }

    public DateTime CreatedAt { get; set; }

    public bool IsExpired() => DateTime.UtcNow > ExpiresAt;
}
