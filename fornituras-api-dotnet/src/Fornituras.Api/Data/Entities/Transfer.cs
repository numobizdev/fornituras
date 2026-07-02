namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Traslado de fornituras entre dos almacenes.
/// </summary>
public class Transfer : BaseEntity
{
    public long OrigenId { get; set; }

    public long DestinoId { get; set; }

    public TransferStatus Status { get; set; } = TransferStatus.ENVIADO;

    public DateTime FechaEnvio { get; set; }

    public DateTime? FechaRecepcion { get; set; }

    public long? CreadoPor { get; set; }

    public long? RecibidoPor { get; set; }

    public string? Observaciones { get; set; }

    public ICollection<TransferItem> Items { get; set; } = new List<TransferItem>();
}
