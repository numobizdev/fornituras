namespace Fornituras.Api.Configuration;

/// <summary>
/// Parámetros de rate limiting (ADR 0010). Reemplaza al `Bucket4jRateLimiter` del backend Java con
/// el limitador nativo de ASP.NET Core (ventana fija). `PermitLimit` peticiones por `WindowSeconds`
/// y por partición (por actor autenticado en `by-codigo`; por IP en el endpoint público).
/// </summary>
public sealed class RateLimitOptions
{
    public RateLimitPolicyOptions ByCodigo { get; set; } = new() { PermitLimit = 30, WindowSeconds = 60 };
    public RateLimitPolicyOptions Public { get; set; } = new() { PermitLimit = 60, WindowSeconds = 60 };
}

public sealed class RateLimitPolicyOptions
{
    public int PermitLimit { get; set; }
    public int WindowSeconds { get; set; }
}

/// <summary>Nombres de las políticas de rate limiting (usados en `[EnableRateLimiting]`).</summary>
public static class RateLimitPolicies
{
    public const string ByCodigo = "by-codigo";
    public const string Public = "public";
}
