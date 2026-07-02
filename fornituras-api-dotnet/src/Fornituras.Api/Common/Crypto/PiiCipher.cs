using System.Security.Cryptography;
using System.Text;

namespace Fornituras.Api.Common.Crypto;

/// <summary>
/// Cifra/descifra PII con AES-256-GCM. El valor persistido es Base64(IV ‖ ciphertext+tag).
/// </summary>
public static class PiiCipher
{
    private const int IvLength = 12;
    private const int TagBits = 128;

    private static byte[]? _key;

    /// <summary>Configura la clave AES (16/24/32 bytes). Idempotente.</summary>
    public static void Configure(ReadOnlySpan<byte> keyBytes)
    {
        _key = keyBytes.ToArray();
    }

    public static bool IsConfigured => _key is not null;

    public static string? Encrypt(string? plain)
    {
        if (plain is null)
        {
            return null;
        }

        var key = RequireKey();
        var iv = new byte[IvLength];
        RandomNumberGenerator.Fill(iv);

        var plainBytes = Encoding.UTF8.GetBytes(plain);
        var ciphertext = new byte[plainBytes.Length];
        var tag = new byte[TagBits / 8];

        using var aesGcm = new AesGcm(key, TagBits / 8);
        aesGcm.Encrypt(iv, plainBytes, ciphertext, tag);

        var combined = new byte[iv.Length + ciphertext.Length + tag.Length];
        Buffer.BlockCopy(iv, 0, combined, 0, iv.Length);
        Buffer.BlockCopy(ciphertext, 0, combined, iv.Length, ciphertext.Length);
        Buffer.BlockCopy(tag, 0, combined, iv.Length + ciphertext.Length, tag.Length);

        return Convert.ToBase64String(combined);
    }

    public static string? Decrypt(string? stored)
    {
        if (stored is null)
        {
            return null;
        }

        var key = RequireKey();
        var combined = Convert.FromBase64String(stored);
        var iv = combined.AsSpan(0, IvLength);
        var tagLength = TagBits / 8;
        var ciphertextLength = combined.Length - IvLength - tagLength;
        var ciphertext = combined.AsSpan(IvLength, ciphertextLength);
        var tag = combined.AsSpan(IvLength + ciphertextLength, tagLength);
        var plainBytes = new byte[ciphertextLength];

        using var aesGcm = new AesGcm(key, tagLength);
        aesGcm.Decrypt(iv, ciphertext, tag, plainBytes);

        return Encoding.UTF8.GetString(plainBytes);
    }

    private static byte[] RequireKey()
    {
        if (_key is null)
        {
            throw new InvalidOperationException(
                "PII_ENCRYPTION_KEY no configurada: el padrón de elementos requiere la clave de cifrado de PII.");
        }

        return _key;
    }
}
