namespace Fornituras.Api.Configuration;

public sealed class SeedAdminOptions
{
    public bool Enabled { get; set; } = true;
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}
