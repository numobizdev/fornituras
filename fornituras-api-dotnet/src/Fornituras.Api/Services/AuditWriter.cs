using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

/// <summary>
/// Puerto de auditoría: persiste eventos append-only en <see cref="AuditLog"/>.
/// </summary>
public interface IAuditWriter
{
    Task RecordAsync(string action, long resourceId, CancellationToken cancellationToken = default);
    Task RecordEventAsync(string action, string? detail, CancellationToken cancellationToken = default);
}

public sealed class AuditWriter(
    ApplicationDbContext db,
    CurrentUserService currentUser,
    IHttpContextAccessor httpContextAccessor,
    ILogger<AuditWriter> logger) : IAuditWriter
{
    public async Task RecordAsync(string action, long resourceId, CancellationToken cancellationToken = default)
    {
        await PersistAsync(action, null, resourceId, null, cancellationToken);
    }

    public async Task RecordEventAsync(string action, string? detail, CancellationToken cancellationToken = default)
    {
        await PersistAsync(action, null, null, detail, cancellationToken);
    }

    private async Task PersistAsync(
        string action,
        string? entidad,
        long? entidadId,
        string? evidencia,
        CancellationToken cancellationToken)
    {
        try
        {
            var actor = currentUser.Email ?? "anonymous";
            long? userId = null;
            if (!string.IsNullOrWhiteSpace(actor) && actor != "anonymous")
            {
                userId = await db.Users.AsNoTracking()
                    .Where(u => u.Email == actor)
                    .Select(u => (long?)u.Id)
                    .FirstOrDefaultAsync(cancellationToken);
            }

            db.AuditLogs.Add(new AuditLog
            {
                Actor = actor,
                UsuarioId = userId,
                Accion = action,
                Entidad = entidad,
                EntidadId = entidadId,
                OccurredAt = DateTime.UtcNow,
                Ip = httpContextAccessor.HttpContext?.Connection.RemoteIpAddress?.ToString(),
                Evidencia = evidencia
            });

            await db.SaveChangesAsync(cancellationToken);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "No se pudo persistir el evento de auditoría action={Action}", action);
        }
    }
}
