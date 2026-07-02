using System.Security.Cryptography;
using System.Text;
using Fornituras.Api.Common.Text;

namespace Fornituras.Api.Common.Crypto;

/// <summary>
/// Calcula el blind index HMAC-SHA256(clave, normalize(valor)) en hex minúsculas.
/// </summary>
public sealed class BlindIndexer
{
    private readonly byte[] _key;

    public BlindIndexer(string key)
    {
        _key = string.IsNullOrWhiteSpace(key)
            ? Array.Empty<byte>()
            : Encoding.UTF8.GetBytes(key.Trim());
    }

    /// <summary>Devuelve el índice hex del valor normalizado, o <c>null</c> si es nulo/vacío.</summary>
    public string? Index(string? raw)
    {
        var normalized = CodeNormalizer.Normalize(raw);
        if (string.IsNullOrEmpty(normalized))
        {
            return null;
        }

        if (_key.Length == 0)
        {
            throw new InvalidOperationException(
                "OFFICER_BLIND_INDEX_KEY no configurada: el padrón requiere la clave del blind index.");
        }

        var digest = HMACSHA256.HashData(_key, Encoding.UTF8.GetBytes(normalized));
        return Convert.ToHexString(digest).ToLowerInvariant();
    }
}
