using Fornituras.Api.Common;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class WarehouseService(
    ApplicationDbContext db,
    CatalogService catalogService,
    IAuditWriter audit) : IWarehouseService
{
    public Task<PageResult<WarehouseSummary>> FindAllAsync(
        bool? active,
        long? tipoItemId,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(active, tipoItemId, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<WarehouseSummary>> FindAllInternalAsync(
        bool? active,
        long? tipoItemId,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Warehouses.AsNoTracking();
        if (active.HasValue)
        {
            query = query.Where(w => w.Active == active.Value);
        }

        if (tipoItemId.HasValue)
        {
            query = query.Where(w => w.TipoItemId == tipoItemId.Value);
        }

        query = query.OrderBy(w => w.Nombre);
        var total = await query.CountAsync(cancellationToken);
        var warehouses = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var tipoNames = await catalogService.ResolveNamesAsync(
            warehouses.Select(w => w.TipoItemId),
            cancellationToken);

        var summaries = warehouses.Select(w => new WarehouseSummary(
            w.Id,
            w.Codigo,
            w.Nombre,
            w.TipoItemId,
            tipoNames.GetValueOrDefault(w.TipoItemId, "?"),
            w.Active)).ToList();

        return PageResult<WarehouseSummary>.From(summaries, total, page, size);
    }

    public async Task<WarehouseDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var warehouse = await db.Warehouses.AsNoTracking()
            .FirstOrDefaultAsync(w => w.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Warehouse not found: {id}");

        var tipoNombre = await catalogService.ResolveNameAsync(warehouse.TipoItemId, cancellationToken) ?? "?";
        var ocupacion = await CountOccupancyAsync(id, cancellationToken);
        double? porcentaje = warehouse.Capacidad is > 0
            ? ocupacion * 100.0 / warehouse.Capacidad.Value
            : null;

        return new WarehouseDetail(
            warehouse.Id,
            warehouse.Codigo,
            warehouse.Nombre,
            warehouse.TipoItemId,
            tipoNombre,
            warehouse.Municipio,
            warehouse.Estado,
            warehouse.Direccion,
            warehouse.Cp,
            warehouse.Latitud,
            warehouse.Longitud,
            warehouse.ResponsableId,
            warehouse.Telefono,
            warehouse.EmailContacto,
            warehouse.Capacidad,
            warehouse.Observaciones,
            warehouse.Active,
            ocupacion,
            porcentaje,
            warehouse.CreatedAt,
            warehouse.UpdatedAt);
    }

    public async Task<WarehouseDetail> CreateAsync(
        WarehouseCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        await catalogService.RequireActiveItemAsync(request.TipoItemId, CatalogCodes.TipoAlmacen, cancellationToken);
        await EnsureUniqueCodigoAsync(request.Codigo, null, cancellationToken);
        await EnsureUniqueNombreAsync(request.Nombre, null, cancellationToken);

        if (request.ResponsableId.HasValue &&
            !await db.Users.AnyAsync(u => u.Id == request.ResponsableId.Value, cancellationToken))
        {
            throw new BadRequestException($"Usuario responsable no encontrado: {request.ResponsableId}");
        }

        var now = DateTime.UtcNow;
        var warehouse = MapFromRequest(request);
        warehouse.Active = true;
        warehouse.CreatedAt = now;
        warehouse.UpdatedAt = now;

        db.Warehouses.Add(warehouse);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_WAREHOUSE", warehouse.Id, cancellationToken);
        return await FindByIdAsync(warehouse.Id, cancellationToken);
    }

    public async Task<WarehouseDetail> UpdateAsync(
        long id,
        WarehouseCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var warehouse = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Warehouse not found: {id}");

        await catalogService.RequireActiveItemAsync(request.TipoItemId, CatalogCodes.TipoAlmacen, cancellationToken);
        await EnsureUniqueCodigoAsync(request.Codigo, id, cancellationToken);
        await EnsureUniqueNombreAsync(request.Nombre, id, cancellationToken);

        if (request.ResponsableId.HasValue &&
            !await db.Users.AnyAsync(u => u.Id == request.ResponsableId.Value, cancellationToken))
        {
            throw new BadRequestException($"Usuario responsable no encontrado: {request.ResponsableId}");
        }

        ApplyRequest(warehouse, request);
        warehouse.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("UPDATE_WAREHOUSE", warehouse.Id, cancellationToken);
        return await FindByIdAsync(warehouse.Id, cancellationToken);
    }

    public async Task DeactivateAsync(long id, CancellationToken cancellationToken = default)
    {
        var warehouse = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Warehouse not found: {id}");

        warehouse.Active = false;
        warehouse.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("DEACTIVATE_WAREHOUSE", warehouse.Id, cancellationToken);
    }

    public async Task DeleteAsync(long id, CancellationToken cancellationToken = default)
    {
        var usage = await CountUsageAsync(id, cancellationToken);
        if (usage > 0)
        {
            throw new ConflictException(
                "No se puede eliminar el almacén porque tiene fornituras o traslados asociados. Desactívelo.");
        }

        var warehouse = await db.Warehouses.FirstOrDefaultAsync(w => w.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Warehouse not found: {id}");

        db.Warehouses.Remove(warehouse);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("DELETE_WAREHOUSE", id, cancellationToken);
    }

    private async Task EnsureUniqueCodigoAsync(string codigo, long? excludeId, CancellationToken cancellationToken)
    {
        var normalized = codigo.Trim().ToUpperInvariant();
        var query = db.Warehouses.Where(w => w.Codigo.ToUpper() == normalized);
        if (excludeId.HasValue)
        {
            query = query.Where(w => w.Id != excludeId.Value);
        }

        if (await query.AnyAsync(cancellationToken))
        {
            throw new ConflictException($"Ya existe un almacén con el código '{codigo.Trim()}'.");
        }
    }

    private async Task EnsureUniqueNombreAsync(string nombre, long? excludeId, CancellationToken cancellationToken)
    {
        var normalized = NameNormalizer.Normalize(nombre);
        var query = db.Warehouses.Where(w => w.NombreNormalizado == normalized);
        if (excludeId.HasValue)
        {
            query = query.Where(w => w.Id != excludeId.Value);
        }

        if (await query.AnyAsync(cancellationToken))
        {
            throw new ConflictException($"Ya existe un almacén con el nombre '{nombre.Trim()}'.");
        }
    }

    private async Task<long> CountOccupancyAsync(long warehouseId, CancellationToken cancellationToken) =>
        await db.Equipment.CountAsync(
            e => e.WarehouseId == warehouseId && e.Status != EntityEquipmentStatus.BAJA_DEFINITIVA,
            cancellationToken);

    private async Task<long> CountUsageAsync(long warehouseId, CancellationToken cancellationToken)
    {
        var equipmentCount = await db.Equipment.CountAsync(e => e.WarehouseId == warehouseId, cancellationToken);
        var transferCount = await db.Transfers.CountAsync(
            t => t.OrigenId == warehouseId || t.DestinoId == warehouseId,
            cancellationToken);
        return equipmentCount + transferCount;
    }

    private static Warehouse MapFromRequest(WarehouseCreateRequest request)
    {
        var warehouse = new Warehouse();
        ApplyRequest(warehouse, request);
        return warehouse;
    }

    private static void ApplyRequest(Warehouse warehouse, WarehouseCreateRequest request)
    {
        warehouse.Codigo = request.Codigo.Trim();
        warehouse.Nombre = request.Nombre.Trim();
        warehouse.NombreNormalizado = NameNormalizer.Normalize(request.Nombre);
        warehouse.TipoItemId = request.TipoItemId;
        warehouse.Municipio = request.Municipio?.Trim();
        warehouse.Estado = request.Estado?.Trim();
        warehouse.Direccion = request.Direccion?.Trim();
        warehouse.Cp = request.Cp?.Trim();
        warehouse.Latitud = request.Latitud;
        warehouse.Longitud = request.Longitud;
        warehouse.ResponsableId = request.ResponsableId;
        warehouse.Telefono = request.Telefono?.Trim();
        warehouse.EmailContacto = request.EmailContacto?.Trim();
        warehouse.Capacidad = request.Capacidad;
        warehouse.Observaciones = request.Observaciones?.Trim();
    }
}
