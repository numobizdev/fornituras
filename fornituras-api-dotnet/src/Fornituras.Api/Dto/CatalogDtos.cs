namespace Fornituras.Api.Dto;

public sealed record CatalogSummary(
    long Id,
    string Code,
    string Nombre,
    string? Descripcion,
    bool System,
    bool Active);

public sealed record CatalogItemSummary(
    long Id,
    long CatalogId,
    string CatalogCode,
    string? Code,
    string Nombre,
    string? Descripcion,
    string? FotoUrl,
    long? ParentItemId,
    int? Orden,
    bool Active);

public sealed record CatalogItemCreateRequest(
    string Nombre,
    string? Code,
    string? Descripcion,
    string? FotoUrl,
    long? ParentItemId,
    int? Orden);
