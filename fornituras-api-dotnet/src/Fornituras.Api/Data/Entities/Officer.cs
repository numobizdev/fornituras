namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Elemento policial del padrón. Contiene PII de alta sensibilidad cifrada a nivel de aplicación.
/// </summary>
public class Officer : BaseEntity
{
    /// <summary>Nombre cifrado (AES-GCM, Base64).</summary>
    public string Nombre { get; set; } = string.Empty;

    /// <summary>Apellido paterno cifrado.</summary>
    public string ApellidoPaterno { get; set; } = string.Empty;

    /// <summary>Apellido materno cifrado.</summary>
    public string? ApellidoMaterno { get; set; }

    /// <summary>Identificador operativo (placa); en claro.</summary>
    public string Placa { get; set; } = string.Empty;

    public string PlacaNormalizada { get; set; } = string.Empty;

    /// <summary>CURP cifrada.</summary>
    public string? Curp { get; set; }

    /// <summary>Blind index HMAC de la CURP.</summary>
    public string? CurpIdx { get; set; }

    /// <summary>RFC cifrado.</summary>
    public string? Rfc { get; set; }

    /// <summary>Blind index HMAC del RFC.</summary>
    public string? RfcIdx { get; set; }

    public long SexoId { get; set; }

    public long? TipoSangreId { get; set; }

    public string? Municipio { get; set; }

    public string? Estado { get; set; }

    public string? FotoUrl { get; set; }

    public bool Active { get; set; } = true;
}
