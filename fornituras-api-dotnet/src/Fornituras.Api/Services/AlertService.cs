using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;
using DtoExpiryStatus = Fornituras.Api.Dto.ExpiryStatus;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;
using EntityExpiryStatus = Fornituras.Api.Data.Entities.ExpiryStatus;

namespace Fornituras.Api.Services;

public sealed class AlertService(ApplicationDbContext db) : IAlertService
{
    public Task<IReadOnlyList<AlertItem>> FindVigenciaAlertsAsync(CancellationToken cancellationToken = default) =>
        VigenciaAlertsAsync(cancellationToken);

    public async Task<IReadOnlyList<AlertItem>> VigenciaAlertsAsync(CancellationToken cancellationToken = default)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var limit = today.AddDays(ExpiryCalculator.WarningWindowDays);

        var equipment = await db.Equipment.AsNoTracking()
            .Where(e =>
                e.FechaVencimiento != null &&
                e.FechaVencimiento <= limit &&
                e.Status != EntityEquipmentStatus.BAJA_DEFINITIVA)
            .ToListAsync(cancellationToken);

        return equipment
            .Select(e =>
            {
                var status = ExpiryCalculator.StatusFor(e.FechaVencimiento, today);
                return new { Equipment = e, Status = status };
            })
            .Where(x => x.Status is not null and not EntityExpiryStatus.VIGENTE)
            .OrderByDescending(x => x.Status == EntityExpiryStatus.CADUCADA)
            .ThenBy(x => x.Equipment.FechaVencimiento)
            .Select(x => new AlertItem(
                x.Equipment.Id,
                x.Equipment.CodigoQr,
                x.Equipment.Descripcion,
                x.Equipment.FechaVencimiento,
                ToDtoExpiry(x.Status!.Value)))
            .ToList();
    }

    private static DtoExpiryStatus ToDtoExpiry(EntityExpiryStatus status) =>
        Enum.Parse<DtoExpiryStatus>(status.ToString());
}
