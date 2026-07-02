using Fornituras.Api.Common;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityCatalogItem = Fornituras.Api.Data.Entities.CatalogItem;

namespace Fornituras.Api.Services;

public sealed class CatalogService(ApplicationDbContext db, IAuditWriter audit) : ICatalogService
{
    public async Task<IReadOnlyList<CatalogSummary>> FindCatalogsAsync(CancellationToken cancellationToken = default)
    {
        return await db.Catalogs.AsNoTracking()
            .OrderBy(c => c.Code)
            .Select(c => new CatalogSummary(
                c.Id, c.Code, c.Nombre, c.Descripcion, c.IsSystem, c.Active))
            .ToListAsync(cancellationToken);
    }

    public Task<PageResult<CatalogItemSummary>> FindItemsAsync(
        string catalogCode,
        bool? active,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindItemsInternalAsync(catalogCode, active, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<CatalogItemSummary>> FindItemsInternalAsync(
        string catalogCode,
        bool? active,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var catalog = await RequireCatalogAsync(catalogCode, cancellationToken);
        var query = db.CatalogItems.AsNoTracking()
            .Where(i => i.CatalogId == catalog.Id);

        if (active.HasValue)
        {
            query = query.Where(i => i.Active == active.Value);
        }

        query = query.OrderBy(i => i.Orden).ThenBy(i => i.Nombre);
        var total = await query.CountAsync(cancellationToken);
        var items = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = items.Select(i => ToSummary(i, catalog.Code)).ToList();
        return PageResult<CatalogItemSummary>.From(summaries, total, page, size);
    }

    public async Task<CatalogItemSummary> FindItemAsync(long itemId, CancellationToken cancellationToken = default)
    {
        var item = await db.CatalogItems.AsNoTracking()
            .Include(i => i.Catalog)
            .FirstOrDefaultAsync(i => i.Id == itemId, cancellationToken)
            ?? throw new NotFoundException($"Catalog item not found: {itemId}");
        return ToSummary(item, item.Catalog.Code);
    }

    public async Task<IReadOnlyList<CatalogItemSummary>> FindActiveItemsAsync(
        string catalogCode,
        long? parentItemId,
        CancellationToken cancellationToken = default)
    {
        var catalog = await RequireCatalogAsync(catalogCode, cancellationToken);
        var query = db.CatalogItems.AsNoTracking()
            .Where(i => i.CatalogId == catalog.Id && i.Active);

        query = parentItemId.HasValue
            ? query.Where(i => i.ParentItemId == parentItemId)
            : query.Where(i => i.ParentItemId == null);

        var items = await query.OrderBy(i => i.Orden).ThenBy(i => i.Nombre).ToListAsync(cancellationToken);
        return items.Select(i => ToSummary(i, catalog.Code)).ToList();
    }

    public async Task<CatalogItemSummary> CreateItemAsync(
        string catalogCode,
        CatalogItemCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var catalog = await RequireCatalogAsync(catalogCode, cancellationToken);
        await EnsureNameUniqueAsync(catalog.Id, request.Nombre, request.ParentItemId, null, cancellationToken);

        var now = DateTime.UtcNow;
        var item = new EntityCatalogItem
        {
            CatalogId = catalog.Id,
            Code = request.Code?.Trim(),
            Nombre = request.Nombre.Trim(),
            NombreNormalizado = NameNormalizer.Normalize(request.Nombre),
            Descripcion = request.Descripcion?.Trim(),
            FotoUrl = request.FotoUrl?.Trim(),
            ParentItemId = request.ParentItemId,
            Orden = request.Orden,
            Active = true,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.CatalogItems.Add(item);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_CATALOG_ITEM", item.Id, cancellationToken);
        return ToSummary(item, catalog.Code);
    }

    public async Task<CatalogItemSummary> UpdateItemAsync(
        long itemId,
        CatalogItemCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var item = await db.CatalogItems.Include(i => i.Catalog)
            .FirstOrDefaultAsync(i => i.Id == itemId, cancellationToken)
            ?? throw new NotFoundException($"Catalog item not found: {itemId}");

        await EnsureNameUniqueAsync(item.CatalogId, request.Nombre, request.ParentItemId, itemId, cancellationToken);

        item.Code = request.Code?.Trim();
        item.Nombre = request.Nombre.Trim();
        item.NombreNormalizado = NameNormalizer.Normalize(request.Nombre);
        item.Descripcion = request.Descripcion?.Trim();
        item.FotoUrl = request.FotoUrl?.Trim();
        item.ParentItemId = request.ParentItemId;
        item.Orden = request.Orden;
        item.UpdatedAt = DateTime.UtcNow;

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("UPDATE_CATALOG_ITEM", item.Id, cancellationToken);
        return ToSummary(item, item.Catalog.Code);
    }

    public async Task DeactivateItemAsync(long itemId, CancellationToken cancellationToken = default)
    {
        var item = await db.CatalogItems.FirstOrDefaultAsync(i => i.Id == itemId, cancellationToken)
            ?? throw new NotFoundException($"Catalog item not found: {itemId}");

        item.Active = false;
        item.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("DEACTIVATE_CATALOG_ITEM", item.Id, cancellationToken);
    }

    public async Task<EntityCatalogItem> RequireActiveItemAsync(
        long itemId,
        string expectedCatalogCode,
        CancellationToken cancellationToken = default)
    {
        var item = await db.CatalogItems.Include(i => i.Catalog)
            .FirstOrDefaultAsync(i => i.Id == itemId, cancellationToken)
            ?? throw new NotFoundException($"Catalog item not found: {itemId}");

        if (!string.Equals(item.Catalog.Code, expectedCatalogCode, StringComparison.OrdinalIgnoreCase))
        {
            throw new BadRequestException(
                $"El ítem {itemId} no pertenece al catálogo {expectedCatalogCode}.");
        }

        if (!item.Active)
        {
            throw new BadRequestException($"El ítem {itemId} está inactivo.");
        }

        return item;
    }

    public async Task<string?> ResolveNameAsync(long itemId, CancellationToken cancellationToken = default)
    {
        return await db.CatalogItems.AsNoTracking()
            .Where(i => i.Id == itemId)
            .Select(i => i.Nombre)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task<Dictionary<long, string>> ResolveNamesAsync(
        IEnumerable<long> itemIds,
        CancellationToken cancellationToken = default)
    {
        var ids = itemIds.Distinct().ToList();
        if (ids.Count == 0)
        {
            return [];
        }

        return await db.CatalogItems.AsNoTracking()
            .Where(i => ids.Contains(i.Id))
            .ToDictionaryAsync(i => i.Id, i => i.Nombre, cancellationToken);
    }

    private async Task<Catalog> RequireCatalogAsync(string catalogCode, CancellationToken cancellationToken)
    {
        return await db.Catalogs.FirstOrDefaultAsync(c => c.Code == catalogCode, cancellationToken)
            ?? throw new NotFoundException($"Catalog not found: {catalogCode}");
    }

    private async Task EnsureNameUniqueAsync(
        long catalogId,
        string nombre,
        long? parentItemId,
        long? excludeId,
        CancellationToken cancellationToken)
    {
        var normalized = NameNormalizer.Normalize(nombre);
        var query = db.CatalogItems.Where(i =>
            i.CatalogId == catalogId &&
            i.NombreNormalizado == normalized &&
            i.ParentItemId == parentItemId);

        if (excludeId.HasValue)
        {
            query = query.Where(i => i.Id != excludeId.Value);
        }

        if (await query.AnyAsync(cancellationToken))
        {
            throw new ConflictException($"Ya existe un ítem con el nombre '{nombre.Trim()}'.");
        }
    }

    private static CatalogItemSummary ToSummary(EntityCatalogItem item, string catalogCode) =>
        new(
            item.Id,
            item.CatalogId,
            catalogCode,
            item.Code,
            item.Nombre,
            item.Descripcion,
            item.FotoUrl,
            item.ParentItemId,
            item.Orden,
            item.Active);
}
