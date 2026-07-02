namespace Fornituras.Api.Dto;

public enum TransferStatus
{
    ENVIADO,
    RECIBIDO,
    CANCELADO
}

public sealed record TransferSummary(
    long Id,
    long OrigenId,
    string OrigenNombre,
    long DestinoId,
    string DestinoNombre,
    TransferStatus Status,
    DateTime? FechaEnvio,
    DateTime? FechaRecepcion,
    long ItemCount);

public sealed record TransferDetail(
    long Id,
    long OrigenId,
    string OrigenNombre,
    long DestinoId,
    string DestinoNombre,
    TransferStatus Status,
    DateTime? FechaEnvio,
    DateTime? FechaRecepcion,
    string? Observaciones,
    IReadOnlyList<TransferItemDetail> Items);

public sealed record TransferItemDetail(
    long EquipmentId,
    string CodigoQr,
    string? Descripcion);

public sealed record TransferCreateRequest(
    long OrigenId,
    long DestinoId,
    IReadOnlyList<long> EquipmentIds,
    string? Observaciones);
