namespace Fornituras.Api.Dto;

public enum EquipmentStatus
{
    DISPONIBLE,
    ASIGNADA,
    EN_MANTENIMIENTO,
    EN_TRASLADO,
    EXTRAVIADA,
    BAJA_DEFINITIVA
}

public enum ExpiryStatus
{
    VIGENTE,
    PROXIMA_A_VENCER,
    CADUCADA
}

public sealed record EquipmentSummary(
    long Id,
    string CodigoQr,
    string? Descripcion,
    string TipoNombre,
    string? TallaEtiqueta,
    string AlmacenNombre,
    EquipmentStatus Status,
    ExpiryStatus Vigencia,
    DateOnly? FechaVencimiento);

public sealed record EquipmentDetail(
    long Id,
    string CodigoQr,
    long EquipmentTypeId,
    string TipoNombre,
    long? SizeId,
    string? TallaEtiqueta,
    long WarehouseId,
    string AlmacenNombre,
    EquipmentStatus Status,
    ExpiryStatus Vigencia,
    string? Descripcion,
    string? Marca,
    string? Modelo,
    string? NivelBalistico,
    string? NumeroInventario,
    DateOnly? FechaFabricacion,
    DateOnly? FechaAdquisicion,
    int? VidaUtilMeses,
    DateOnly? FechaVencimiento,
    string? Observaciones,
    string? FotoUrl,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record EquipmentCreateRequest(
    string CodigoQr,
    long EquipmentTypeId,
    long? SizeId,
    long WarehouseId,
    string? Descripcion,
    string? Marca,
    string? Modelo,
    string? NivelBalistico,
    string? NumeroInventario,
    DateOnly? FechaFabricacion,
    DateOnly? FechaAdquisicion,
    int? VidaUtilMeses,
    DateOnly? FechaVencimiento,
    string? Observaciones,
    string? FotoUrl);

public sealed record BatchCreateRequest(
    long EquipmentTypeId,
    long? SizeId,
    long WarehouseId,
    string? Descripcion,
    string? Marca,
    string? Modelo,
    string? NivelBalistico,
    DateOnly? FechaFabricacion,
    DateOnly? FechaAdquisicion,
    int? VidaUtilMeses,
    DateOnly? FechaVencimiento,
    string? Observaciones,
    IReadOnlyList<string> Codigos);

public sealed record StatusChangeRequest(EquipmentStatus Status);
