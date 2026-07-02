using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/catalogs")]
public sealed class CatalogController(ICatalogService catalogService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<CatalogSummary>>>> GetCatalogs(
        CancellationToken cancellationToken)
    {
        var catalogs = await catalogService.FindCatalogsAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<CatalogSummary>>.Ok(catalogs));
    }

    [HttpGet("{code}/items")]
    public async Task<ActionResult<ApiResponse<PageResult<CatalogItemSummary>>>> GetItems(
        string code,
        [FromQuery] bool? active,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await catalogService.FindItemsAsync(code, active, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<CatalogItemSummary>>.Ok(page));
    }

    [HttpGet("items/{itemId:long}")]
    public async Task<ActionResult<ApiResponse<CatalogItemSummary>>> GetItem(
        long itemId,
        CancellationToken cancellationToken)
    {
        var item = await catalogService.FindItemAsync(itemId, cancellationToken);
        return Ok(ApiResponse<CatalogItemSummary>.Ok(item));
    }

    [HttpGet("{code}/items/active")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<CatalogItemSummary>>>> GetActiveItems(
        string code,
        [FromQuery] long? parentItemId,
        CancellationToken cancellationToken)
    {
        var items = await catalogService.FindActiveItemsAsync(code, parentItemId, cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<CatalogItemSummary>>.Ok(items));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPost("{code}/items")]
    public async Task<ActionResult<ApiResponse<CatalogItemSummary>>> CreateItem(
        string code,
        [FromBody] CatalogItemCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await catalogService.CreateItemAsync(code, request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created,
            ApiResponse<CatalogItemSummary>.Ok(created, "Valor de catálogo creado."));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPut("items/{itemId:long}")]
    public async Task<ActionResult<ApiResponse<CatalogItemSummary>>> UpdateItem(
        long itemId,
        [FromBody] CatalogItemCreateRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await catalogService.UpdateItemAsync(itemId, request, cancellationToken);
        return Ok(ApiResponse<CatalogItemSummary>.Ok(updated, "Valor de catálogo actualizado."));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPatch("items/{itemId:long}/deactivate")]
    public async Task<ActionResult<ApiResponse<object?>>> DeactivateItem(
        long itemId,
        CancellationToken cancellationToken)
    {
        await catalogService.DeactivateItemAsync(itemId, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Valor de catálogo desactivado."));
    }
}
