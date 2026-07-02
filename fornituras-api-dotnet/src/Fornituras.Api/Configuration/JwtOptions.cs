namespace Fornituras.Api.Configuration;

public sealed class JwtOptions
{
    public string Secret { get; set; } = string.Empty;
    public long ExpirationMs { get; set; } = 86_400_000;
}
