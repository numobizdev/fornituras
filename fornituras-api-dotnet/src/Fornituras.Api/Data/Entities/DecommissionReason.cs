namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Motivo de baja (catálogo controlado).
/// </summary>
public class DecommissionReason : BaseEntity
{
    public string Nombre { get; set; } = string.Empty;

    public bool Active { get; set; } = true;
}
