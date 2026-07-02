using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityTransferStatus = Fornituras.Api.Data.Entities.TransferStatus;
using DtoTransferStatus = Fornituras.Api.Dto.TransferStatus;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class TransferService(
    ApplicationDbContext db,
    CurrentUserService currentUser,
    IAuditWriter audit) : ITransferService
{
    public Task<PageResult<TransferSummary>> FindAllAsync(
        long? origenId,
        long? destinoId,
        DtoTransferStatus? status,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(origenId, destinoId, status, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<TransferSummary>> FindAllInternalAsync(
        long? origenId,
        long? destinoId,
        DtoTransferStatus? status,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Transfers.AsNoTracking();

        if (origenId.HasValue)
        {
            query = query.Where(t => t.OrigenId == origenId.Value);
        }

        if (destinoId.HasValue)
        {
            query = query.Where(t => t.DestinoId == destinoId.Value);
        }

        if (status.HasValue)
        {
            query = query.Where(t => t.Status == ToEntityStatus(status.Value));
        }

        query = query.OrderByDescending(t => t.FechaEnvio);
        var total = await query.CountAsync(cancellationToken);
        var transfers = await query
            .Include(t => t.Items)
            .Skip(page * size)
            .Take(size)
            .ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(transfers, cancellationToken);
        return PageResult<TransferSummary>.From(summaries, total, page, size);
    }

    public async Task<TransferDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var transfer = await db.Transfers.AsNoTracking()
            .Include(t => t.Items)
            .FirstOrDefaultAsync(t => t.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Transfer not found: {id}");

        return await MapDetailAsync(transfer, cancellationToken);
    }

    public async Task<TransferDetail> CreateAsync(
        TransferCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        if (request.OrigenId == request.DestinoId)
        {
            throw new BadRequestException("El almacén de origen y destino deben ser distintos.");
        }

        var origen = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == request.OrigenId, cancellationToken)
            ?? throw new BadRequestException($"Almacén origen no encontrado: {request.OrigenId}");

        var destino = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == request.DestinoId, cancellationToken)
            ?? throw new BadRequestException($"Almacén destino no encontrado: {request.DestinoId}");

        if (!destino.Active)
        {
            throw new BadRequestException("El almacén destino está inactivo.");
        }

        if (request.EquipmentIds.Count == 0)
        {
            throw new BadRequestException("Debe incluir al menos una fornitura.");
        }

        var equipmentList = await db.Equipment
            .Where(e => request.EquipmentIds.Contains(e.Id))
            .ToListAsync(cancellationToken);

        if (equipmentList.Count != request.EquipmentIds.Count)
        {
            throw new BadRequestException("Una o más fornituras no existen.");
        }

        foreach (var equipment in equipmentList)
        {
            if (equipment.Status != EntityEquipmentStatus.DISPONIBLE)
            {
                throw new ConflictException(
                    $"La fornitura {equipment.CodigoQr} no está disponible para traslado.");
            }

            if (equipment.WarehouseId != origen.Id)
            {
                throw new ConflictException(
                    $"La fornitura {equipment.CodigoQr} no pertenece al almacén de origen.");
            }
        }

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        var transfer = new Transfer
        {
            OrigenId = request.OrigenId,
            DestinoId = request.DestinoId,
            Status = EntityTransferStatus.ENVIADO,
            FechaEnvio = now,
            CreadoPor = userId,
            Observaciones = request.Observaciones?.Trim(),
            CreatedAt = now,
            UpdatedAt = now,
            Items = equipmentList.Select(e => new TransferItem
            {
                EquipmentId = e.Id,
                CreatedAt = now,
                UpdatedAt = now
            }).ToList()
        };

        foreach (var equipment in equipmentList)
        {
            equipment.Status = EntityEquipmentStatus.EN_TRASLADO;
            equipment.UpdatedAt = now;
        }

        db.Transfers.Add(transfer);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_TRANSFER", transfer.Id, cancellationToken);
        return await FindByIdAsync(transfer.Id, cancellationToken);
    }

    public async Task<TransferDetail> ReceiveAsync(long id, CancellationToken cancellationToken = default)
    {
        var transfer = await db.Transfers
            .Include(t => t.Items)
            .FirstOrDefaultAsync(t => t.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Transfer not found: {id}");

        if (transfer.Status != EntityTransferStatus.ENVIADO)
        {
            throw new ConflictException("El traslado no está en estado ENVIADO.");
        }

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        transfer.Status = EntityTransferStatus.RECIBIDO;
        transfer.FechaRecepcion = now;
        transfer.RecibidoPor = userId;
        transfer.UpdatedAt = now;

        var equipmentIds = transfer.Items.Select(i => i.EquipmentId).ToList();
        var equipmentList = await db.Equipment.Where(e => equipmentIds.Contains(e.Id)).ToListAsync(cancellationToken);

        foreach (var equipment in equipmentList)
        {
            equipment.Status = EntityEquipmentStatus.DISPONIBLE;
            equipment.WarehouseId = transfer.DestinoId;
            equipment.UpdatedAt = now;
        }

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("RECEIVE_TRANSFER", transfer.Id, cancellationToken);
        return await FindByIdAsync(transfer.Id, cancellationToken);
    }

    public async Task<TransferDetail> CancelAsync(long id, CancellationToken cancellationToken = default)
    {
        var transfer = await db.Transfers
            .Include(t => t.Items)
            .FirstOrDefaultAsync(t => t.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Transfer not found: {id}");

        if (transfer.Status != EntityTransferStatus.ENVIADO)
        {
            throw new ConflictException("El traslado no está en estado ENVIADO.");
        }

        var now = DateTime.UtcNow;
        transfer.Status = EntityTransferStatus.CANCELADO;
        transfer.UpdatedAt = now;

        var equipmentIds = transfer.Items.Select(i => i.EquipmentId).ToList();
        var equipmentList = await db.Equipment.Where(e => equipmentIds.Contains(e.Id)).ToListAsync(cancellationToken);

        foreach (var equipment in equipmentList)
        {
            equipment.Status = EntityEquipmentStatus.DISPONIBLE;
            equipment.UpdatedAt = now;
        }

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CANCEL_TRANSFER", transfer.Id, cancellationToken);
        return await FindByIdAsync(transfer.Id, cancellationToken);
    }

    private async Task<IReadOnlyList<TransferSummary>> MapSummariesAsync(
        IReadOnlyList<Transfer> transfers,
        CancellationToken cancellationToken)
    {
        var warehouseIds = transfers.SelectMany(t => new[] { t.OrigenId, t.DestinoId }).Distinct();
        var names = await db.Warehouses.AsNoTracking()
            .Where(w => warehouseIds.Contains(w.Id))
            .ToDictionaryAsync(w => w.Id, w => w.Nombre, cancellationToken);

        return transfers.Select(t => new TransferSummary(
            t.Id,
            t.OrigenId,
            names.GetValueOrDefault(t.OrigenId, "?"),
            t.DestinoId,
            names.GetValueOrDefault(t.DestinoId, "?"),
            ToDtoStatus(t.Status),
            t.FechaEnvio,
            t.FechaRecepcion,
            t.Items?.Count ?? 0)).ToList();
    }

    private async Task<TransferDetail> MapDetailAsync(Transfer transfer, CancellationToken cancellationToken)
    {
        var origen = await db.Warehouses.AsNoTracking()
            .Where(w => w.Id == transfer.OrigenId)
            .Select(w => w.Nombre)
            .FirstAsync(cancellationToken);
        var destino = await db.Warehouses.AsNoTracking()
            .Where(w => w.Id == transfer.DestinoId)
            .Select(w => w.Nombre)
            .FirstAsync(cancellationToken);

        var equipmentIds = transfer.Items.Select(i => i.EquipmentId).ToList();
        var equipmentMap = await db.Equipment.AsNoTracking()
            .Where(e => equipmentIds.Contains(e.Id))
            .ToDictionaryAsync(e => e.Id, cancellationToken);

        var items = transfer.Items.Select(i =>
        {
            equipmentMap.TryGetValue(i.EquipmentId, out var eq);
            return new TransferItemDetail(i.EquipmentId, eq?.CodigoQr ?? "?", eq?.Descripcion);
        }).ToList();

        return new TransferDetail(
            transfer.Id,
            transfer.OrigenId,
            origen,
            transfer.DestinoId,
            destino,
            ToDtoStatus(transfer.Status),
            transfer.FechaEnvio,
            transfer.FechaRecepcion,
            transfer.Observaciones,
            items);
    }

    private static EntityTransferStatus ToEntityStatus(DtoTransferStatus status) =>
        Enum.Parse<EntityTransferStatus>(status.ToString());

    private static DtoTransferStatus ToDtoStatus(EntityTransferStatus status) =>
        Enum.Parse<DtoTransferStatus>(status.ToString());
}
