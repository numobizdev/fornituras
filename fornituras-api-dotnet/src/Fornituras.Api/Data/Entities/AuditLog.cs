namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Evento de la bitácora de auditoría (append-only; sin <c>updated_at</c>).
/// </summary>
public class AuditLog
{
    public long Id { get; set; }

    public long? UsuarioId { get; set; }

    public string? Actor { get; set; }

    public string Accion { get; set; } = string.Empty;

    public string? Entidad { get; set; }

    public long? EntidadId { get; set; }

    public DateTime OccurredAt { get; set; }

    public string? Ip { get; set; }

    public string? Evidencia { get; set; }

    public string? PrevHash { get; set; }
}
