namespace Fornituras.Api.Dto;

public sealed record DashboardSummary(
    long Total,
    long Disponibles,
    long Asignadas,
    long ProximasAVencer,
    long Caducadas,
    long EnMantenimiento);
