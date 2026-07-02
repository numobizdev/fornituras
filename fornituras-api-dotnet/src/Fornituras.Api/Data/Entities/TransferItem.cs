namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Línea de un traslado: una fornitura incluida en él.
/// </summary>
public class TransferItem : BaseEntity
{
    public long TransferId { get; set; }

    public Transfer Transfer { get; set; } = null!;

    public long EquipmentId { get; set; }
}
