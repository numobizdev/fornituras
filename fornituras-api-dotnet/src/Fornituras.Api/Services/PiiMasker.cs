namespace Fornituras.Api.Services;

/// <summary>
/// Enmascara identificadores de PII (CURP/RFC): primeros 4 caracteres visibles.
/// </summary>
public static class PiiMasker
{
    private const int VisiblePrefix = 4;

    public static string? Mask(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return value;
        }

        var trimmed = value.Trim();
        if (trimmed.Length <= VisiblePrefix)
        {
            return new string('•', trimmed.Length);
        }

        return trimmed[..VisiblePrefix] + new string('•', trimmed.Length - VisiblePrefix);
    }
}
