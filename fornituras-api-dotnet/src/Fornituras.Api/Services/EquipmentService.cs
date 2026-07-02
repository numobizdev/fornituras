using Fornituras.Api.Common;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;
using EntityExpiryStatus = Fornituras.Api.Data.Entities.ExpiryStatus;
using DtoEquipmentStatus = Fornituras.Api.Dto.EquipmentStatus;
using DtoExpiryStatus = Fornituras.Api.Dto.ExpiryStatus;

namespace Fornituras.Api.Services;

public sealed class EquipmentService(
    ApplicationDbContext db,
    CatalogService catalogService,
    IAuditWriter audit) : IEquipmentService
{
    public Task<PageResult<EquipmentSummary>> FindAllAsync(
        string? q,
        DtoEquipmentStatus? status,
        long? equipmentTypeId,
        long? sizeId,
        long? warehouseId,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(q, status, equipmentTypeId, sizeId, warehouseId, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<EquipmentSummary>> FindAllInternalAsync(
        string? q,
        DtoEquipmentStatus? status,
        long? equipmentTypeId,
        long? sizeId,
        long? warehouseId,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Equipment.AsNoTracking();

        if (!string.IsNullOrWhiteSpace(q))
        {
            var normalized = CodeNormalizer.Normalize(q);
            var upperQ = q.Trim().ToUpperInvariant();
            query = query.Where(e =>
                e.CodigoNormalizado.Contains(normalized) ||
                (e.Descripcion != null && e.Descripcion.ToUpper().Contains(upperQ)));
        }

        if (status.HasValue)
        {
            query = query.Where(e => e.Status == ToEntityStatus(status.Value));
        }

        if (equipmentTypeId.HasValue)
        {
            query = query.Where(e => e.EquipmentTypeId == equipmentTypeId.Value);
        }

        if (sizeId.HasValue)
        {
            query = query.Where(e => e.SizeId == sizeId.Value);
        }

        if (warehouseId.HasValue)
        {
            query = query.Where(e => e.WarehouseId == warehouseId.Value);
        }

        query = query.OrderBy(e => e.CodigoQr);
        var total = await query.CountAsync(cancellationToken);
        var items = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(items, cancellationToken);
        return PageResult<EquipmentSummary>.From(summaries, total, page, size);
    }

    public async Task<EquipmentDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var equipment = await db.Equipment.AsNoTracking()
            .FirstOrDefaultAsync(e => e.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {id}");
        return await MapDetailAsync(equipment, cancellationToken);
    }

    public async Task<EquipmentDetail> FindByCodigoAsync(string codigo, CancellationToken cancellationToken = default)
    {
        var normalized = CodeNormalizer.Normalize(codigo);
        var equipment = await db.Equipment.AsNoTracking()
            .FirstOrDefaultAsync(e => e.CodigoNormalizado == normalized, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {codigo}");
        return await MapDetailAsync(equipment, cancellationToken);
    }

    public async Task<EquipmentDetail> CreateAsync(
        EquipmentCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var equipment = await BuildEquipmentAsync(request, cancellationToken);
        equipment.Status = EntityEquipmentStatus.DISPONIBLE;
        var now = DateTime.UtcNow;
        equipment.CreatedAt = now;
        equipment.UpdatedAt = now;

        db.Equipment.Add(equipment);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_EQUIPMENT", equipment.Id, cancellationToken);
        return await FindByIdAsync(equipment.Id, cancellationToken);
    }

    public async Task<IReadOnlyList<EquipmentDetail>> CreateBatchAsync(
        BatchCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        if (request.Codigos.Count == 0)
        {
            throw new BadRequestException("Debe proporcionar al menos un código.");
        }

        var normalizedCodes = request.Codigos
            .Select(c => CodeNormalizer.Normalize(c))
            .Where(c => !string.IsNullOrEmpty(c))
            .ToList();

        if (normalizedCodes.Count != request.Codigos.Count)
        {
            throw new BadRequestException("Todos los códigos deben ser válidos.");
        }

        if (normalizedCodes.Distinct().Count() != normalizedCodes.Count)
        {
            throw new ConflictException("Hay códigos duplicados en el lote.");
        }

        var existing = await db.Equipment
            .Where(e => normalizedCodes.Contains(e.CodigoNormalizado))
            .Select(e => e.CodigoNormalizado)
            .ToListAsync(cancellationToken);

        if (existing.Count > 0)
        {
            throw new ConflictException($"Códigos ya registrados: {string.Join(", ", existing)}");
        }

        await ValidateBatchReferencesAsync(request, cancellationToken);

        var now = DateTime.UtcNow;
        var results = new List<Equipment>();

        foreach (var (codigo, index) in request.Codigos.Select((c, i) => (c, i)))
        {
            var createRequest = new EquipmentCreateRequest(
                codigo,
                request.EquipmentTypeId,
                request.SizeId,
                request.WarehouseId,
                request.Descripcion,
                request.Marca,
                request.Modelo,
                request.NivelBalistico,
                null,
                request.FechaFabricacion,
                request.FechaAdquisicion,
                request.VidaUtilMeses,
                request.FechaVencimiento,
                request.Observaciones,
                null);

            var equipment = await BuildEquipmentAsync(createRequest, cancellationToken);
            equipment.Status = EntityEquipmentStatus.DISPONIBLE;
            equipment.CreatedAt = now;
            equipment.UpdatedAt = now;
            results.Add(equipment);
        }

        db.Equipment.AddRange(results);
        await db.SaveChangesAsync(cancellationToken);

        foreach (var equipment in results)
        {
            await audit.RecordAsync("CREATE_EQUIPMENT", equipment.Id, cancellationToken);
        }

        var details = new List<EquipmentDetail>();
        foreach (var equipment in results)
        {
            details.Add(await MapDetailAsync(equipment, cancellationToken));
        }

        return details;
    }

    public async Task<EquipmentDetail> UpdateAsync(
        long id,
        EquipmentCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var equipment = await db.Equipment.FirstOrDefaultAsync(e => e.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {id}");

        var newNormalized = CodeNormalizer.Normalize(request.CodigoQr);
        if (newNormalized != equipment.CodigoNormalizado)
        {
            throw new ConflictException("El código QR no se puede modificar una vez registrado.");
        }

        await ValidateReferencesAsync(request, cancellationToken);
        ApplyMutableFields(equipment, request);
        equipment.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("UPDATE_EQUIPMENT", equipment.Id, cancellationToken);
        return await FindByIdAsync(equipment.Id, cancellationToken);
    }

    public Task<EquipmentDetail> ChangeStatusAsync(
        long id,
        StatusChangeRequest request,
        CancellationToken cancellationToken = default) =>
        ChangeStatusInternalAsync(id, request.Status, cancellationToken);

    private async Task<EquipmentDetail> ChangeStatusInternalAsync(
        long id,
        DtoEquipmentStatus newStatus,
        CancellationToken cancellationToken = default)
    {
        var equipment = await db.Equipment.FirstOrDefaultAsync(e => e.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {id}");

        var entityStatus = ToEntityStatus(newStatus);
        await AssertStatusChangeAllowedAsync(equipment, entityStatus, cancellationToken);

        equipment.Status = entityStatus;
        equipment.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("STATUS_CHANGE_EQUIPMENT", equipment.Id, cancellationToken);
        return await FindByIdAsync(equipment.Id, cancellationToken);
    }

    private async Task AssertStatusChangeAllowedAsync(
        Equipment equipment,
        EntityEquipmentStatus newStatus,
        CancellationToken cancellationToken)
    {
        if (newStatus == EntityEquipmentStatus.BAJA_DEFINITIVA)
        {
            if (await HasActiveAssignmentAsync(equipment.Id, cancellationToken))
            {
                throw new ConflictException("No se puede dar de baja una fornitura con asignación vigente.");
            }

            if (await HasOngoingTransferAsync(equipment.Id, cancellationToken))
            {
                throw new ConflictException("No se puede dar de baja una fornitura en traslado.");
            }
        }

        if (newStatus == EntityEquipmentStatus.EN_TRASLADO &&
            await HasActiveAssignmentAsync(equipment.Id, cancellationToken))
        {
            throw new ConflictException("No se puede trasladar una fornitura con asignación vigente.");
        }
    }

    private async Task<bool> HasActiveAssignmentAsync(long equipmentId, CancellationToken cancellationToken) =>
        await db.Assignments.AnyAsync(
            a => a.EquipmentId == equipmentId && a.FechaDevolucion == null,
            cancellationToken);

    private async Task<bool> HasOngoingTransferAsync(long equipmentId, CancellationToken cancellationToken) =>
        await db.TransferItems.AnyAsync(
            ti => ti.EquipmentId == equipmentId &&
                  ti.Transfer!.Status == Data.Entities.TransferStatus.ENVIADO,
            cancellationToken);

    private async Task<Equipment> BuildEquipmentAsync(
        EquipmentCreateRequest request,
        CancellationToken cancellationToken)
    {
        var normalized = CodeNormalizer.Normalize(request.CodigoQr);
        if (string.IsNullOrEmpty(normalized))
        {
            throw new BadRequestException("El código QR es obligatorio.");
        }

        if (await db.Equipment.AnyAsync(e => e.CodigoNormalizado == normalized, cancellationToken))
        {
            throw new ConflictException($"Código ya registrado: {request.CodigoQr.Trim()}");
        }

        await ValidateReferencesAsync(request, cancellationToken);

        var equipment = new Equipment
        {
            CodigoQr = request.CodigoQr.Trim().ToUpperInvariant(),
            CodigoNormalizado = normalized
        };
        ApplyMutableFields(equipment, request);
        return equipment;
    }

    private async Task ValidateReferencesAsync(
        EquipmentCreateRequest request,
        CancellationToken cancellationToken)
    {
        await catalogService.RequireActiveItemAsync(
            request.EquipmentTypeId, CatalogCodes.TipoPrenda, cancellationToken);

        if (request.SizeId.HasValue)
        {
            await catalogService.RequireActiveItemAsync(
                request.SizeId.Value, CatalogCodes.Talla, cancellationToken);
        }

        var warehouse = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == request.WarehouseId, cancellationToken)
            ?? throw new BadRequestException($"Almacén no encontrado: {request.WarehouseId}");

        if (!warehouse.Active)
        {
            throw new BadRequestException($"El almacén {request.WarehouseId} está inactivo.");
        }
    }

    private async Task ValidateBatchReferencesAsync(
        BatchCreateRequest request,
        CancellationToken cancellationToken)
    {
        await catalogService.RequireActiveItemAsync(
            request.EquipmentTypeId, CatalogCodes.TipoPrenda, cancellationToken);

        if (request.SizeId.HasValue)
        {
            await catalogService.RequireActiveItemAsync(
                request.SizeId.Value, CatalogCodes.Talla, cancellationToken);
        }

        var warehouse = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == request.WarehouseId, cancellationToken)
            ?? throw new BadRequestException($"Almacén no encontrado: {request.WarehouseId}");

        if (!warehouse.Active)
        {
            throw new BadRequestException($"El almacén {request.WarehouseId} está inactivo.");
        }
    }

    private static void ApplyMutableFields(Equipment equipment, EquipmentCreateRequest request)
    {
        equipment.EquipmentTypeId = request.EquipmentTypeId;
        equipment.SizeId = request.SizeId;
        equipment.WarehouseId = request.WarehouseId;
        equipment.Descripcion = request.Descripcion?.Trim();
        equipment.Marca = request.Marca?.Trim();
        equipment.Modelo = request.Modelo?.Trim();
        equipment.NivelBalistico = request.NivelBalistico?.Trim();
        equipment.NumeroInventario = request.NumeroInventario?.Trim();
        equipment.FechaFabricacion = request.FechaFabricacion;
        equipment.FechaAdquisicion = request.FechaAdquisicion;
        equipment.VidaUtilMeses = request.VidaUtilMeses;
        equipment.FechaVencimiento = request.FechaVencimiento ??
            ComputeExpiry(request.FechaFabricacion, request.VidaUtilMeses);
        equipment.Observaciones = request.Observaciones?.Trim();
        equipment.FotoUrl = request.FotoUrl?.Trim();
    }

    private static DateOnly? ComputeExpiry(DateOnly? fabrication, int? lifeMonths)
    {
        if (fabrication is null || lifeMonths is null)
        {
            return null;
        }

        return fabrication.Value.AddMonths(lifeMonths.Value);
    }

    private async Task<IReadOnlyList<EquipmentSummary>> MapSummariesAsync(
        IReadOnlyList<Equipment> items,
        CancellationToken cancellationToken)
    {
        var tipoIds = items.Select(i => i.EquipmentTypeId).Distinct();
        var sizeIds = items.Where(i => i.SizeId.HasValue).Select(i => i.SizeId!.Value).Distinct();
        var warehouseIds = items.Select(i => i.WarehouseId).Distinct();

        var tipoNames = await catalogService.ResolveNamesAsync(tipoIds, cancellationToken);
        var sizeNames = await catalogService.ResolveNamesAsync(sizeIds, cancellationToken);
        var warehouseNames = await db.Warehouses.AsNoTracking()
            .Where(w => warehouseIds.Contains(w.Id))
            .ToDictionaryAsync(w => w.Id, w => w.Nombre, cancellationToken);

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        return items.Select(e =>
        {
            var vigencia = ExpiryCalculator.StatusFor(e.FechaVencimiento, today);
            return new EquipmentSummary(
                e.Id,
                e.CodigoQr,
                e.Descripcion,
                tipoNames.GetValueOrDefault(e.EquipmentTypeId, "?"),
                e.SizeId.HasValue ? sizeNames.GetValueOrDefault(e.SizeId.Value) : null,
                warehouseNames.GetValueOrDefault(e.WarehouseId, "?"),
                ToDtoStatus(e.Status),
                vigencia.HasValue ? ToDtoExpiry(vigencia.Value) : DtoExpiryStatus.VIGENTE,
                e.FechaVencimiento);
        }).ToList();
    }

    private async Task<EquipmentDetail> MapDetailAsync(Equipment e, CancellationToken cancellationToken)
    {
        var tipoNombre = await catalogService.ResolveNameAsync(e.EquipmentTypeId, cancellationToken) ?? "?";
        string? talla = e.SizeId.HasValue
            ? await catalogService.ResolveNameAsync(e.SizeId.Value, cancellationToken)
            : null;
        var almacen = await db.Warehouses.AsNoTracking()
            .Where(w => w.Id == e.WarehouseId)
            .Select(w => w.Nombre)
            .FirstOrDefaultAsync(cancellationToken) ?? "?";

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var vigencia = ExpiryCalculator.StatusFor(e.FechaVencimiento, today);

        return new EquipmentDetail(
            e.Id,
            e.CodigoQr,
            e.EquipmentTypeId,
            tipoNombre,
            e.SizeId,
            talla,
            e.WarehouseId,
            almacen,
            ToDtoStatus(e.Status),
            vigencia.HasValue ? ToDtoExpiry(vigencia.Value) : DtoExpiryStatus.VIGENTE,
            e.Descripcion,
            e.Marca,
            e.Modelo,
            e.NivelBalistico,
            e.NumeroInventario,
            e.FechaFabricacion,
            e.FechaAdquisicion,
            e.VidaUtilMeses,
            e.FechaVencimiento,
            e.Observaciones,
            e.FotoUrl,
            e.CreatedAt,
            e.UpdatedAt);
    }

    private static EntityEquipmentStatus ToEntityStatus(DtoEquipmentStatus status) =>
        Enum.Parse<EntityEquipmentStatus>(status.ToString());

    private static DtoEquipmentStatus ToDtoStatus(EntityEquipmentStatus status) =>
        Enum.Parse<DtoEquipmentStatus>(status.ToString());

    private static DtoExpiryStatus ToDtoExpiry(EntityExpiryStatus status) =>
        Enum.Parse<DtoExpiryStatus>(status.ToString());
}
