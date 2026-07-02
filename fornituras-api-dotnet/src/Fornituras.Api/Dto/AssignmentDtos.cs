namespace Fornituras.Api.Dto;

public sealed record AssignmentSummary(
    long Id,
    long EquipmentId,
    string CodigoQr,
    string? EquipmentDescripcion,
    long OfficerId,
    string ElementoNombre,
    string Placa,
    DateTime FechaAsignacion,
    DateTime? FechaDevolucion,
    bool Vigente);

public sealed record AssignRequest(
    long EquipmentId,
    long OfficerId,
    string? Observaciones);

public sealed record ReassignRequest(
    long EquipmentId,
    long NewOfficerId,
    string? Observaciones);
