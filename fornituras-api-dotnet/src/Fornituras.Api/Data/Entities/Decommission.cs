namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Baja definitiva de una fornitura.
/// </summary>
public class Decommission : BaseEntity
{
    public long EquipmentId { get; set; }

    public long MotivoId { get; set; }

    public DateOnly Fecha { get; set; }

    public long? Responsable { get; set; }

    public string? Observaciones { get; set; }
}
