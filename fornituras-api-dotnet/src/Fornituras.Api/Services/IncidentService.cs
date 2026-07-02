using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityIncidentStatus = Fornituras.Api.Data.Entities.IncidentStatus;
using EntityIncidentType = Fornituras.Api.Data.Entities.IncidentType;
using DtoIncidentStatus = Fornituras.Api.Dto.IncidentStatus;
using DtoIncidentType = Fornituras.Api.Dto.IncidentType;
using DtoEquipmentStatus = Fornituras.Api.Dto.EquipmentStatus;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class IncidentService(
    ApplicationDbContext db,
    EquipmentService equipmentService,
    CurrentUserService currentUser,
    IAuditWriter audit) : IIncidentService
{
    public Task<PageResult<IncidentSummary>> FindAllAsync(
        DtoIncidentStatus? estado,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(estado, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<IncidentSummary>> FindAllInternalAsync(
        DtoIncidentStatus? estado,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Incidents.AsNoTracking();
        if (estado.HasValue)
        {
            query = query.Where(i => i.Estado == ToEntityStatus(estado.Value));
        }

        query = query.OrderByDescending(i => i.FechaReporte);
        var total = await query.CountAsync(cancellationToken);
        var incidents = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(incidents, cancellationToken);
        return PageResult<IncidentSummary>.From(summaries, total, page, size);
    }

    public async Task<IncidentSummary> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var incident = await db.Incidents.AsNoTracking()
            .FirstOrDefaultAsync(i => i.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Incident not found: {id}");
        return (await MapSummariesAsync([incident], cancellationToken)).Single();
    }

    public Task<IncidentSummary> CreateAsync(
        IncidentCreateRequest request,
        CancellationToken cancellationToken = default) =>
        ReportAsync(request, cancellationToken);

    public async Task<IncidentSummary> ReportAsync(
        IncidentCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var equipment = await db.Equipment.FirstOrDefaultAsync(e => e.Id == request.EquipmentId, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {request.EquipmentId}");

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        var incident = new Incident
        {
            EquipmentId = request.EquipmentId,
            Tipo = ToEntityType(request.Tipo),
            Descripcion = request.Descripcion.Trim(),
            Estado = EntityIncidentStatus.ABIERTA,
            FechaReporte = now,
            ReportadoPor = userId,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Incidents.Add(incident);
        await db.SaveChangesAsync(cancellationToken);

        await ApplyEquipmentStatusOnReportAsync(equipment, request.Tipo, cancellationToken);
        await audit.RecordAsync("REPORT_INCIDENT", incident.Id, cancellationToken);
        return (await MapSummariesAsync([incident], cancellationToken)).Single();
    }

    public async Task<IncidentSummary> UpdateAsync(
        long id,
        IncidentUpdateRequest request,
        CancellationToken cancellationToken = default)
    {
        var incident = await db.Incidents.FirstOrDefaultAsync(i => i.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Incident not found: {id}");

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var newStatus = ToEntityStatus(request.Estado);
        incident.Estado = newStatus;
        incident.ActualizadoPor = userId;
        incident.UpdatedAt = DateTime.UtcNow;

        if (newStatus is EntityIncidentStatus.RESUELTA or EntityIncidentStatus.CERRADA &&
            incident.FechaResolucion is null)
        {
            incident.FechaResolucion = DateTime.UtcNow;
        }

        await db.SaveChangesAsync(cancellationToken);

        if (newStatus is EntityIncidentStatus.RESUELTA or EntityIncidentStatus.CERRADA)
        {
            var equipment = await db.Equipment.FirstOrDefaultAsync(
                e => e.Id == incident.EquipmentId, cancellationToken);
            if (equipment is not null &&
                equipment.Status is EntityEquipmentStatus.EN_MANTENIMIENTO or EntityEquipmentStatus.EXTRAVIADA)
            {
                await equipmentService.ChangeStatusAsync(
                    equipment.Id, new StatusChangeRequest(DtoEquipmentStatus.DISPONIBLE), cancellationToken);
            }
        }

        await audit.RecordAsync("UPDATE_INCIDENT", incident.Id, cancellationToken);
        return (await MapSummariesAsync([incident], cancellationToken)).Single();
    }

    private async Task ApplyEquipmentStatusOnReportAsync(
        Equipment equipment,
        DtoIncidentType tipo,
        CancellationToken cancellationToken)
    {
        if (equipment.Status is not (EntityEquipmentStatus.DISPONIBLE or EntityEquipmentStatus.ASIGNADA))
        {
            return;
        }

        var newStatus = tipo == DtoIncidentType.EXTRAVIO
            ? DtoEquipmentStatus.EXTRAVIADA
            : DtoEquipmentStatus.EN_MANTENIMIENTO;

        await equipmentService.ChangeStatusAsync(
            equipment.Id, new StatusChangeRequest(newStatus), cancellationToken);
    }

    private async Task<IReadOnlyList<IncidentSummary>> MapSummariesAsync(
        IReadOnlyList<Incident> incidents,
        CancellationToken cancellationToken)
    {
        var equipmentIds = incidents.Select(i => i.EquipmentId).Distinct().ToList();
        var codes = await db.Equipment.AsNoTracking()
            .Where(e => equipmentIds.Contains(e.Id))
            .ToDictionaryAsync(e => e.Id, e => e.CodigoQr, cancellationToken);

        return incidents.Select(i => new IncidentSummary(
            i.Id,
            i.EquipmentId,
            codes.GetValueOrDefault(i.EquipmentId, "?"),
            ToDtoType(i.Tipo),
            i.Descripcion,
            ToDtoStatus(i.Estado),
            i.FechaReporte,
            i.FechaResolucion)).ToList();
    }

    private static EntityIncidentStatus ToEntityStatus(DtoIncidentStatus status) =>
        Enum.Parse<EntityIncidentStatus>(status.ToString());

    private static DtoIncidentStatus ToDtoStatus(EntityIncidentStatus status) =>
        Enum.Parse<DtoIncidentStatus>(status.ToString());

    private static EntityIncidentType ToEntityType(DtoIncidentType type) =>
        Enum.Parse<EntityIncidentType>(type.ToString());

    private static DtoIncidentType ToDtoType(EntityIncidentType type) =>
        Enum.Parse<DtoIncidentType>(type.ToString());
}
