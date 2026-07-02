namespace Fornituras.Api.Common.Text;

/// <summary>
/// Normaliza códigos opacos (QR/serie): recorta, elimina espacios/guiones y pasa a mayúsculas.
/// </summary>
public static class CodeNormalizer
{
    public static string Normalize(string? raw)
    {
        if (raw is null)
        {
            return string.Empty;
        }

        return System.Text.RegularExpressions.Regex.Replace(raw, @"[\s-]+", string.Empty)
            .ToUpperInvariant();
    }
}
