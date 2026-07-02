using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

public sealed class AuditLogService(ApplicationDbContext db)
{
    public async Task<PageResult<AuditLogSummary>> QueryAsync(
        string? actor,
        string? accion,
        string? entidad,
        DateTime? desde,
        DateTime? hasta,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.AuditLogs.AsNoTracking();

        if (!string.IsNullOrWhiteSpace(actor))
        {
            var pattern = actor.Trim().ToLowerInvariant();
            query = query.Where(a => a.Actor != null && a.Actor.ToLower().Contains(pattern));
        }

        if (!string.IsNullOrWhiteSpace(accion))
        {
            var upper = accion.Trim().ToUpperInvariant();
            query = query.Where(a => a.Accion == upper);
        }

        if (!string.IsNullOrWhiteSpace(entidad))
        {
            var upper = entidad.Trim().ToUpperInvariant();
            query = query.Where(a => a.Entidad == upper);
        }

        if (desde.HasValue)
        {
            query = query.Where(a => a.OccurredAt >= desde.Value);
        }

        if (hasta.HasValue)
        {
            query = query.Where(a => a.OccurredAt <= hasta.Value);
        }

        query = query.OrderByDescending(a => a.OccurredAt);
        var total = await query.CountAsync(cancellationToken);
        var logs = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);

        var summaries = logs.Select(a => new AuditLogSummary(
            a.Id,
            a.UsuarioId,
            a.Actor,
            a.Accion,
            a.Entidad ?? string.Empty,
            a.EntidadId,
            a.OccurredAt,
            a.Ip,
            a.Evidencia)).ToList();

        return PageResult<AuditLogSummary>.From(summaries, total, page, size);
    }
}
