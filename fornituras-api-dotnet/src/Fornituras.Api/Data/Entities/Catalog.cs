namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Cabecera de un catálogo administrable (p. ej. TIPO_PRENDA, TALLA, TIPO_ALMACEN).
/// </summary>
public class Catalog : BaseEntity
{
    public string Code { get; set; } = string.Empty;

    public string Nombre { get; set; } = string.Empty;

    public string? Descripcion { get; set; }

    public bool IsSystem { get; set; }

    public bool Active { get; set; } = true;

    public ICollection<CatalogItem> Items { get; set; } = new List<CatalogItem>();
}
