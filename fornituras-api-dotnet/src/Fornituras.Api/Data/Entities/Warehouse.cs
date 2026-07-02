namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Almacén: ubicación física de resguardo de fornituras.
/// </summary>
public class Warehouse : BaseEntity
{
    public string Codigo { get; set; } = string.Empty;

    public string Nombre { get; set; } = string.Empty;

    public string NombreNormalizado { get; set; } = string.Empty;

    /// <summary>FK a un valor del catálogo TIPO_ALMACEN.</summary>
    public long TipoItemId { get; set; }

    public string? Municipio { get; set; }

    public string? Estado { get; set; }

    public string? Direccion { get; set; }

    public string? Cp { get; set; }

    public decimal? Latitud { get; set; }

    public decimal? Longitud { get; set; }

    public long? ResponsableId { get; set; }

    public string? Telefono { get; set; }

    public string? EmailContacto { get; set; }

    public int? Capacidad { get; set; }

    public string? Observaciones { get; set; }

    public bool Active { get; set; } = true;
}
