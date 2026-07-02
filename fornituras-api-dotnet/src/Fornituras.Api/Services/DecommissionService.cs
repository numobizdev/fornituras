using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using DtoEquipmentStatus = Fornituras.Api.Dto.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class DecommissionService(
    ApplicationDbContext db,
    EquipmentService equipmentService,
    CatalogService catalogService,
    CurrentUserService currentUser,
    IAuditWriter audit) : IDecommissionService
{
    public Task<DecommissionSummary> CreateAsync(
        DecommissionRequest request,
        CancellationToken cancellationToken = default) =>
        DecommissionAsync(request, cancellationToken);

    public async Task<IReadOnlyList<DecommissionReasonItem>> FindReasonsAsync(
        CancellationToken cancellationToken = default)
    {
        return await db.DecommissionReasons.AsNoTracking()
            .Where(r => r.Active)
            .OrderBy(r => r.Nombre)
            .Select(r => new DecommissionReasonItem(r.Id, r.Nombre))
            .ToListAsync(cancellationToken);
    }

    public async Task<DecommissionSummary> DecommissionAsync(
        DecommissionRequest request,
        CancellationToken cancellationToken = default)
    {
        var equipmentDetail = await equipmentService.FindByCodigoAsync(request.Codigo, cancellationToken);

        if (equipmentDetail.Status == DtoEquipmentStatus.BAJA_DEFINITIVA)
        {
            throw new ConflictException("La fornitura ya está dada de baja.");
        }

        var motivo = await db.DecommissionReasons.FirstOrDefaultAsync(
            r => r.Id == request.MotivoId && r.Active, cancellationToken)
            ?? throw new BadRequestException($"Motivo de baja no encontrado: {request.MotivoId}");

        await equipmentService.ChangeStatusAsync(
            equipmentDetail.Id,
            new StatusChangeRequest(DtoEquipmentStatus.BAJA_DEFINITIVA),
            cancellationToken);

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        var record = new Decommission
        {
            EquipmentId = equipmentDetail.Id,
            MotivoId = motivo.Id,
            Fecha = DateOnly.FromDateTime(DateTime.UtcNow),
            Responsable = userId,
            Observaciones = request.Observaciones?.Trim(),
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Decommissions.Add(record);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("DECOMMISSION_EQUIPMENT", record.Id, cancellationToken);

        var tipoNombre = equipmentDetail.TipoNombre;
        return new DecommissionSummary(
            record.Id,
            equipmentDetail.Id,
            equipmentDetail.CodigoQr,
            equipmentDetail.Descripcion,
            tipoNombre,
            motivo.Id,
            motivo.Nombre,
            record.Fecha,
            record.Responsable ?? 0,
            record.Observaciones);
    }

    public Task<PageResult<DecommissionSummary>> FindAllAsync(
        DateOnly? fechaDesde,
        DateOnly? fechaHasta,
        long? equipmentTypeId,
        long? motivoId,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(fechaDesde, fechaHasta, equipmentTypeId, motivoId, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<DecommissionSummary>> FindAllInternalAsync(
        DateOnly? fechaDesde,
        DateOnly? fechaHasta,
        long? equipmentTypeId,
        long? motivoId,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Decommissions.AsNoTracking();

        if (fechaDesde.HasValue)
        {
            query = query.Where(d => d.Fecha >= fechaDesde.Value);
        }

        if (fechaHasta.HasValue)
        {
            query = query.Where(d => d.Fecha <= fechaHasta.Value);
        }

        if (motivoId.HasValue)
        {
            query = query.Where(d => d.MotivoId == motivoId.Value);
        }

        if (equipmentTypeId.HasValue)
        {
            query = query.Where(d =>
                db.Equipment.Any(e => e.Id == d.EquipmentId && e.EquipmentTypeId == equipmentTypeId.Value));
        }

        query = query.OrderByDescending(d => d.Fecha);
        var total = await query.CountAsync(cancellationToken);
        var records = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(records, cancellationToken);
        return PageResult<DecommissionSummary>.From(summaries, total, page, size);
    }

    private async Task<IReadOnlyList<DecommissionSummary>> MapSummariesAsync(
        IReadOnlyList<Decommission> records,
        CancellationToken cancellationToken)
    {
        var equipmentIds = records.Select(r => r.EquipmentId).Distinct().ToList();
        var motivoIds = records.Select(r => r.MotivoId).Distinct().ToList();

        var equipmentMap = await db.Equipment.AsNoTracking()
            .Where(e => equipmentIds.Contains(e.Id))
            .ToDictionaryAsync(e => e.Id, cancellationToken);

        var motivoMap = await db.DecommissionReasons.AsNoTracking()
            .Where(r => motivoIds.Contains(r.Id))
            .ToDictionaryAsync(r => r.Id, r => r.Nombre, cancellationToken);

        var tipoIds = equipmentMap.Values.Select(e => e.EquipmentTypeId).Distinct();
        var tipoNames = await catalogService.ResolveNamesAsync(tipoIds, cancellationToken);

        return records.Select(r =>
        {
            equipmentMap.TryGetValue(r.EquipmentId, out var eq);
            return new DecommissionSummary(
                r.Id,
                r.EquipmentId,
                eq?.CodigoQr ?? "?",
                eq?.Descripcion,
                eq is not null ? tipoNames.GetValueOrDefault(eq.EquipmentTypeId, "?") : "?",
                r.MotivoId,
                motivoMap.GetValueOrDefault(r.MotivoId, "?"),
                r.Fecha,
                r.Responsable ?? 0,
                r.Observaciones);
        }).ToList();
    }
}
