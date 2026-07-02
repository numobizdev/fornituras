namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Asignación (resguardo): relación fornitura↔elemento en el tiempo.
/// <c>FechaDevolucion == null</c> significa vigente.
/// </summary>
public class Assignment : BaseEntity
{
    public long EquipmentId { get; set; }

    public long OfficerId { get; set; }

    public DateTime FechaAsignacion { get; set; }

    /// <summary>NULL = asignación vigente; con valor = devuelta (histórico).</summary>
    public DateTime? FechaDevolucion { get; set; }

    public long? AsignadoPor { get; set; }

    public long? RecibidoPor { get; set; }

    public string? FirmaUrl { get; set; }

    public string? Observaciones { get; set; }
}
