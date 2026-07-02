using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Configuration;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/equipment")]
public sealed class EquipmentController(IEquipmentService equipmentService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<EquipmentSummary>>>> GetAll(
        [FromQuery] string? q,
        [FromQuery] EquipmentStatus? status,
        [FromQuery] long? equipmentTypeId,
        [FromQuery] long? sizeId,
        [FromQuery] long? warehouseId,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await equipmentService.FindAllAsync(
            q, status, equipmentTypeId, sizeId, warehouseId, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<EquipmentSummary>>.Ok(page));
    }

    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<EquipmentDetail>>> GetById(long id, CancellationToken cancellationToken)
    {
        var equipment = await equipmentService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<EquipmentDetail>.Ok(equipment));
    }

    [EnableRateLimiting(RateLimitPolicies.ByCodigo)]
    [HttpGet("by-codigo/{codigo}")]
    public async Task<ActionResult<ApiResponse<EquipmentDetail>>> GetByCodigo(
        string codigo,
        CancellationToken cancellationToken)
    {
        var equipment = await equipmentService.FindByCodigoAsync(codigo, cancellationToken);
        return Ok(ApiResponse<EquipmentDetail>.Ok(equipment));
    }

    [Authorize(Roles = RolePolicy.WriteInventory)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<EquipmentDetail>>> Create(
        [FromBody] EquipmentCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await equipmentService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<EquipmentDetail>.Ok(created));
    }

    [Authorize(Roles = RolePolicy.WriteInventory)]
    [HttpPost("batch")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<EquipmentDetail>>>> CreateBatch(
        [FromBody] BatchCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await equipmentService.CreateBatchAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<IReadOnlyList<EquipmentDetail>>.Ok(created));
    }

    [Authorize(Roles = RolePolicy.WriteInventory)]
    [HttpPut("{id:long}")]
    public async Task<ActionResult<ApiResponse<EquipmentDetail>>> Update(
        long id,
        [FromBody] EquipmentCreateRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await equipmentService.UpdateAsync(id, request, cancellationToken);
        return Ok(ApiResponse<EquipmentDetail>.Ok(updated));
    }

    [Authorize(Roles = RolePolicy.WriteInventory)]
    [HttpPatch("{id:long}/status")]
    public async Task<ActionResult<ApiResponse<EquipmentDetail>>> ChangeStatus(
        long id,
        [FromBody] StatusChangeRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await equipmentService.ChangeStatusAsync(id, request, cancellationToken);
        return Ok(ApiResponse<EquipmentDetail>.Ok(updated));
    }
}
