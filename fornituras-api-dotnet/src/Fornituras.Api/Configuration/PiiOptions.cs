namespace Fornituras.Api.Configuration;

public sealed class PiiOptions
{
    public string EncryptionKey { get; set; } = string.Empty;
    public string BlindIndexKey { get; set; } = string.Empty;
}
