using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/warehouses")]
public sealed class WarehousesController(IWarehouseService warehouseService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<WarehouseSummary>>>> GetAll(
        [FromQuery] bool? active,
        [FromQuery] long? tipoItemId,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await warehouseService.FindAllAsync(active, tipoItemId, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<WarehouseSummary>>.Ok(page));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<WarehouseDetail>>> GetById(long id, CancellationToken cancellationToken)
    {
        var warehouse = await warehouseService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<WarehouseDetail>.Ok(warehouse));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<WarehouseDetail>>> Create(
        [FromBody] WarehouseCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await warehouseService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created,
            ApiResponse<WarehouseDetail>.Ok(created, "Almacén creado."));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPut("{id:long}")]
    public async Task<ActionResult<ApiResponse<WarehouseDetail>>> Update(
        long id,
        [FromBody] WarehouseCreateRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await warehouseService.UpdateAsync(id, request, cancellationToken);
        return Ok(ApiResponse<WarehouseDetail>.Ok(updated, "Almacén actualizado."));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpPatch("{id:long}/deactivate")]
    public async Task<ActionResult<ApiResponse<object?>>> Deactivate(long id, CancellationToken cancellationToken)
    {
        await warehouseService.DeactivateAsync(id, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Almacén desactivado."));
    }

    [Authorize(Roles = RolePolicy.ManageConfig)]
    [HttpDelete("{id:long}")]
    public async Task<ActionResult<ApiResponse<object?>>> Delete(long id, CancellationToken cancellationToken)
    {
        await warehouseService.DeleteAsync(id, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Almacén eliminado."));
    }
}
