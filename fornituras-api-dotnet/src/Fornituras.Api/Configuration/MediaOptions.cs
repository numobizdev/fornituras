namespace Fornituras.Api.Configuration;

/// <summary>
/// Opciones del módulo de fotos (017): almacenamiento cifrado en filesystem, límites de peso y
/// dimensiones, y el gate legal de la foto de elemento (PII, ADR 0003).
/// </summary>
public sealed class MediaOptions
{
    /// <summary>Directorio raíz donde se guardan los objetos cifrados; fuera del repo (ADR 0017).</summary>
    public string StoragePath { get; set; } = "media-store";

    /// <summary>Peso máximo aceptado del archivo original (bytes).</summary>
    public long MaxSizeBytes { get; set; } = 5 * 1024 * 1024;

    /// <summary>Ancho máximo permitido (px) tras decodificar.</summary>
    public int MaxWidth { get; set; } = 4096;

    /// <summary>Alto máximo permitido (px) tras decodificar.</summary>
    public int MaxHeight { get; set; } = 4096;

    /// <summary>
    /// Gate legal (ADR 0003): mientras esté deshabilitado, la captura de foto de elemento (PII)
    /// se rechaza aunque el rol esté autorizado. Se habilita solo con base legal confirmada.
    /// </summary>
    public bool OfficerPhotoEnabled { get; set; }
}
