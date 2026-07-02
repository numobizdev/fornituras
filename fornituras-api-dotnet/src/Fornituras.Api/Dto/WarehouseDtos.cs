namespace Fornituras.Api.Dto;

public sealed record WarehouseSummary(
    long Id,
    string Codigo,
    string Nombre,
    long TipoItemId,
    string TipoNombre,
    bool Active);

public sealed record WarehouseDetail(
    long Id,
    string Codigo,
    string Nombre,
    long TipoItemId,
    string TipoNombre,
    string? Municipio,
    string? Estado,
    string? Direccion,
    string? Cp,
    decimal? Latitud,
    decimal? Longitud,
    long? ResponsableId,
    string? Telefono,
    string? EmailContacto,
    int? Capacidad,
    string? Observaciones,
    bool Active,
    long Ocupacion,
    double? PorcentajeOcupacion,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record WarehouseCreateRequest(
    string Codigo,
    string Nombre,
    long TipoItemId,
    string? Municipio,
    string? Estado,
    string? Direccion,
    string? Cp,
    decimal? Latitud,
    decimal? Longitud,
    long? ResponsableId,
    string? Telefono,
    string? EmailContacto,
    int? Capacidad,
    string? Observaciones);
