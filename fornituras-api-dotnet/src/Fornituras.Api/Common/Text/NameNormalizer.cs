using System.Globalization;
using System.Text;

namespace Fornituras.Api.Common.Text;

/// <summary>
/// Normaliza nombres: recorta, colapsa espacios, quita acentos y pasa a minúsculas.
/// </summary>
public static class NameNormalizer
{
    public static string Normalize(string? raw)
    {
        if (raw is null)
        {
            return string.Empty;
        }

        var collapsed = System.Text.RegularExpressions.Regex.Replace(raw.Trim(), @"\s+", " ");
        var normalized = collapsed.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder(normalized.Length);

        foreach (var ch in normalized)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(ch) != UnicodeCategory.NonSpacingMark)
            {
                builder.Append(ch);
            }
        }

        return builder.ToString().Normalize(NormalizationForm.FormC).ToLowerInvariant();
    }
}
