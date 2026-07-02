namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Contexto de una foto: fija si el objeto es PII y activa el RBAC/gating del servidor.
/// </summary>
public enum MediaContext
{
    /// <summary>Foto de una fornitura/equipo (no PII).</summary>
    EQUIPMENT,

    /// <summary>Foto de un tipo/valor de catálogo (no PII).</summary>
    EQUIPMENT_TYPE,

    /// <summary>Foto de un elemento policial (PII de alta sensibilidad).</summary>
    OFFICER
}

/// <summary>
/// Referencia de una foto almacenada. El binario vive cifrado en filesystem (ADR 0017); esta fila
/// solo guarda metadatos opacos. El identificador es un GUID para no exponer secuencia enumerable.
/// </summary>
public class MediaAsset
{
    public Guid Id { get; set; } = Guid.NewGuid();

    /// <summary>Ruta relativa del objeto cifrado bajo el directorio de almacenamiento.</summary>
    public string StorageKey { get; set; } = string.Empty;

    /// <summary>Content-type final tras el saneo (image/jpeg, image/png, image/webp).</summary>
    public string ContentType { get; set; } = string.Empty;

    /// <summary>Tamaño en bytes del objeto saneado (antes de cifrar).</summary>
    public long SizeBytes { get; set; }

    /// <summary>Verdadero si la foto contiene PII (contexto de elemento).</summary>
    public bool IsPii { get; set; }

    public MediaContext Context { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
