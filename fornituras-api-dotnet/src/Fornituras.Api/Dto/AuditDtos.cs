namespace Fornituras.Api.Dto;

public sealed record AuditLogSummary(
    long Id,
    long? UsuarioId,
    string? Actor,
    string Accion,
    string Entidad,
    long? EntidadId,
    DateTime OccurredAt,
    string? Ip,
    string? Evidencia);
