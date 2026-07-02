namespace Fornituras.Api.Dto;

public sealed record DecommissionSummary(
    long Id,
    long EquipmentId,
    string EquipmentCodigo,
    string? Descripcion,
    string TipoNombre,
    long MotivoId,
    string MotivoNombre,
    DateOnly Fecha,
    long Responsable,
    string? Observaciones);

public sealed record DecommissionRequest(
    string Codigo,
    long MotivoId,
    string? Observaciones);

public sealed record DecommissionReasonItem(long Id, string Nombre);
