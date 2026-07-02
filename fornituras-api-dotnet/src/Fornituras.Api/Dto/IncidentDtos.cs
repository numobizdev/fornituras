namespace Fornituras.Api.Dto;

public enum IncidentType
{
    DANO,
    FALLA,
    EXTRAVIO,
    MANTENIMIENTO
}

public enum IncidentStatus
{
    ABIERTA,
    EN_PROCESO,
    RESUELTA,
    CERRADA
}

public sealed record IncidentSummary(
    long Id,
    long EquipmentId,
    string EquipmentCodigo,
    IncidentType Tipo,
    string Descripcion,
    IncidentStatus Estado,
    DateTime FechaReporte,
    DateTime? FechaResolucion);

public sealed record IncidentCreateRequest(
    long EquipmentId,
    IncidentType Tipo,
    string Descripcion);

public sealed record IncidentUpdateRequest(IncidentStatus Estado);

public sealed record AlertItem(
    long EquipmentId,
    string EquipmentCodigo,
    string? Descripcion,
    DateOnly? FechaVencimiento,
    ExpiryStatus ExpiryStatus);
