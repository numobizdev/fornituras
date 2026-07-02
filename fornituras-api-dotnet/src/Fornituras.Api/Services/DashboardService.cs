using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class DashboardService(ApplicationDbContext db) : IDashboardService
{
    public Task<DashboardSummary> GetSummaryAsync(CancellationToken cancellationToken = default) =>
        SummaryAsync(cancellationToken);

    public async Task<DashboardSummary> SummaryAsync(CancellationToken cancellationToken = default)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var warningLimit = today.AddDays(ExpiryCalculator.WarningWindowDays);

        var statusCounts = await db.Equipment.AsNoTracking()
            .GroupBy(e => e.Status)
            .Select(g => new { Status = g.Key, Count = g.LongCount() })
            .ToListAsync(cancellationToken);

        var total = statusCounts.Sum(x => x.Count);
        var disponibles = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.DISPONIBLE)?.Count ?? 0;
        var asignadas = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.ASIGNADA)?.Count ?? 0;
        var enMantenimiento = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.EN_MANTENIMIENTO)?.Count ?? 0;

        var caducadas = await db.Equipment.AsNoTracking()
            .CountAsync(
                e => e.FechaVencimiento != null &&
                     e.FechaVencimiento < today &&
                     e.Status != EntityEquipmentStatus.BAJA_DEFINITIVA,
                cancellationToken);

        var proximas = await db.Equipment.AsNoTracking()
            .CountAsync(
                e => e.FechaVencimiento != null &&
                     e.FechaVencimiento >= today &&
                     e.FechaVencimiento <= warningLimit &&
                     e.Status != EntityEquipmentStatus.BAJA_DEFINITIVA,
                cancellationToken);

        return new DashboardSummary(total, disponibles, asignadas, proximas, caducadas, enMantenimiento);
    }
}
