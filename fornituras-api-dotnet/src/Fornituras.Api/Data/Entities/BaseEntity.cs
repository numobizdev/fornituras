namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Entidad base con identificador y marcas de auditoría de creación/actualización.
/// </summary>
public abstract class BaseEntity
{
    public long Id { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
