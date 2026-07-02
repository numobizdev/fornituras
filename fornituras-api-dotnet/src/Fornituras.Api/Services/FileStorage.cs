using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Configuration;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Services;

/// <summary>
/// Puerto de almacenamiento binario por clave opaca. Desacopla el servicio de fotos del backend de
/// almacenamiento (filesystem local hoy; podría ser objeto/nube mañana). Ver ADR 0017.
/// </summary>
public interface IFileStorage
{
    Task StoreAsync(string storageKey, byte[] content, CancellationToken cancellationToken = default);
    Task<byte[]> LoadAsync(string storageKey, CancellationToken cancellationToken = default);
    Task DeleteAsync(string storageKey, CancellationToken cancellationToken = default);
}

/// <summary>
/// Adaptador que persiste objetos **cifrados en reposo** (AES-256-GCM, misma clave que la PII) bajo
/// un directorio fuera del repo. En disco solo hay <c>IV ‖ ciphertext ‖ tag</c>: nunca la imagen en
/// claro. La <c>storageKey</c> la genera el servidor (no viene del cliente), evitando path traversal.
/// </summary>
public sealed class LocalEncryptedFileStorage : IFileStorage
{
    private readonly string _root;

    public LocalEncryptedFileStorage(IOptions<AppOptions> options)
    {
        _root = Path.GetFullPath(options.Value.Media.StoragePath);
        Directory.CreateDirectory(_root);
    }

    public async Task StoreAsync(string storageKey, byte[] content, CancellationToken cancellationToken = default)
    {
        var path = ResolvePath(storageKey);
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var encrypted = PiiCipher.EncryptBytes(content);
        await File.WriteAllBytesAsync(path, encrypted, cancellationToken);
    }

    public async Task<byte[]> LoadAsync(string storageKey, CancellationToken cancellationToken = default)
    {
        var path = ResolvePath(storageKey);
        var encrypted = await File.ReadAllBytesAsync(path, cancellationToken);
        return PiiCipher.DecryptBytes(encrypted);
    }

    public Task DeleteAsync(string storageKey, CancellationToken cancellationToken = default)
    {
        var path = ResolvePath(storageKey);
        if (File.Exists(path))
        {
            File.Delete(path);
        }

        return Task.CompletedTask;
    }

    private string ResolvePath(string storageKey)
    {
        var full = Path.GetFullPath(Path.Combine(_root, storageKey));
        // Defensa en profundidad: nunca escribir/leer fuera de la raíz configurada.
        if (!full.StartsWith(_root + Path.DirectorySeparatorChar, StringComparison.Ordinal) && full != _root)
        {
            throw new InvalidOperationException("Ruta de almacenamiento inválida.");
        }

        return full;
    }
}
