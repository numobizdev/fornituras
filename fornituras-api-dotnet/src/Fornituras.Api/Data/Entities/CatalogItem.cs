namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Valor de un catálogo, opcionalmente jerárquico mediante <see cref="ParentItem"/>.
/// </summary>
public class CatalogItem : BaseEntity
{
    public long CatalogId { get; set; }

    public Catalog Catalog { get; set; } = null!;

    /// <summary>Clave opcional estable dentro del catálogo (p. ej. CENTRAL/REGIONAL para TIPO_ALMACEN).</summary>
    public string? Code { get; set; }

    public string Nombre { get; set; } = string.Empty;

    public string NombreNormalizado { get; set; } = string.Empty;

    public string? Descripcion { get; set; }

    public string? FotoUrl { get; set; }

    public long? ParentItemId { get; set; }

    public CatalogItem? ParentItem { get; set; }

    public ICollection<CatalogItem> ChildItems { get; set; } = new List<CatalogItem>();

    public int? Orden { get; set; }

    public bool Active { get; set; } = true;
}
