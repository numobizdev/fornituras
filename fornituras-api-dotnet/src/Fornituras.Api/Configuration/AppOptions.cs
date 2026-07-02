namespace Fornituras.Api.Configuration;

/// <summary>
/// Opciones de aplicación (equivalente a {@code app.*} en Spring Boot).
/// </summary>
public sealed class AppOptions
{
    public const string SectionName = "App";

    public JwtOptions Jwt { get; set; } = new();
    public QrOptions Qr { get; set; } = new();
    public PiiOptions Pii { get; set; } = new();
    public SeedOptions Seed { get; set; } = new();
    public CorsOptions Cors { get; set; } = new();
}

public sealed class SeedOptions
{
    public SeedAdminOptions Admin { get; set; } = new();
}
