namespace Fornituras.Api.Dto;

public sealed record OfficerSummary(
    long Id,
    string NombreCompleto,
    string Placa,
    string? SexoNombre,
    string? TipoSangreEtiqueta,
    string? Municipio,
    string? Estado,
    string? FotoUrl,
    bool Active);

public sealed record OfficerDetail(
    long Id,
    string Nombre,
    string ApellidoPaterno,
    string? ApellidoMaterno,
    string NombreCompleto,
    string Placa,
    long SexoId,
    string? SexoNombre,
    long? TipoSangreId,
    string? TipoSangreEtiqueta,
    string? Municipio,
    string? Estado,
    string? Curp,
    string? Rfc,
    bool PiiEnmascarada,
    string? FotoUrl,
    bool Active,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record OfficerCreateRequest(
    string Nombre,
    string ApellidoPaterno,
    string? ApellidoMaterno,
    string Placa,
    long SexoId,
    long? TipoSangreId,
    string? Municipio,
    string? Estado,
    string? Curp,
    string? Rfc,
    string? FotoUrl);

/// <summary>
/// Elemento de catálogo para selectores del padrón (sexo, tipo de sangre, etc.).
/// </summary>
public sealed record CatalogItem(long Id, string Etiqueta);
