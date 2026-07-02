namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Fornitura: equipo físico controlado por el inventario.
/// </summary>
public class Equipment : BaseEntity
{
    public string CodigoQr { get; set; } = string.Empty;

    public string CodigoNormalizado { get; set; } = string.Empty;

    public long EquipmentTypeId { get; set; }

    public long? SizeId { get; set; }

    public long WarehouseId { get; set; }

    public EquipmentStatus Status { get; set; } = EquipmentStatus.DISPONIBLE;

    public string? Descripcion { get; set; }

    public string? Marca { get; set; }

    public string? Modelo { get; set; }

    public string? NivelBalistico { get; set; }

    public string? NumeroInventario { get; set; }

    public DateOnly? FechaFabricacion { get; set; }

    public DateOnly? FechaAdquisicion { get; set; }

    public int? VidaUtilMeses { get; set; }

    public DateOnly? FechaVencimiento { get; set; }

    public string? Observaciones { get; set; }

    public string? FotoUrl { get; set; }
}
