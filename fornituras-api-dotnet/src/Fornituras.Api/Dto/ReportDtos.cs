namespace Fornituras.Api.Dto;

public sealed record ActiveAssignmentRow(
    long AssignmentId,
    long EquipmentId,
    string CodigoQr,
    string? EquipmentDescripcion,
    long OfficerId,
    string ElementoNombre,
    string Placa,
    string? Curp,
    string? Rfc,
    string? Municipio,
    string? Estado,
    bool PiiMasked,
    DateTime FechaAsignacion);

public sealed record ActiveAssignmentFilter(
    string? Qr,
    string? Nombre,
    string? Rfc,
    string? Placa,
    string? Curp,
    string? Municipio);

public sealed record ReportTotals(
    long TotalFornituras,
    long Disponibles,
    long Asignadas,
    long EnMantenimiento,
    long ConIncidencia,
    long Baja,
    long TotalElementos);

/// <summary>
/// Reportes operativos predefinidos por estado de fornitura.
/// </summary>
public enum PredefinedReportType
{
    INVENTARIO_GENERAL,
    DISPONIBLES,
    ASIGNADAS,
    MANTENIMIENTO,
    BAJA
}
