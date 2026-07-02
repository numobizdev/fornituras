namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Incidencia registrada sobre una fornitura.
/// </summary>
public class Incident : BaseEntity
{
    public long EquipmentId { get; set; }

    public IncidentType Tipo { get; set; }

    public string Descripcion { get; set; } = string.Empty;

    public IncidentStatus Estado { get; set; } = IncidentStatus.ABIERTA;

    public DateTime FechaReporte { get; set; }

    public DateTime? FechaResolucion { get; set; }

    public long? ReportadoPor { get; set; }

    public long? ActualizadoPor { get; set; }
}
