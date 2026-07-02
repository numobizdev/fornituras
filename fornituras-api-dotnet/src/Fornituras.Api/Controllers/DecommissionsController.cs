using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/decommissions")]
public sealed class DecommissionsController(IDecommissionService decommissionService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<DecommissionSummary>>>> GetAll(
        [FromQuery] DateOnly? fechaDesde,
        [FromQuery] DateOnly? fechaHasta,
        [FromQuery] long? tipoId,
        [FromQuery] long? motivoId,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await decommissionService.FindAllAsync(
            fechaDesde, fechaHasta, tipoId, motivoId, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<DecommissionSummary>>.Ok(page));
    }

    [HttpGet("reasons")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<DecommissionReasonItem>>>> GetReasons(
        CancellationToken cancellationToken)
    {
        var reasons = await decommissionService.FindReasonsAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<DecommissionReasonItem>>.Ok(reasons));
    }

    [Authorize(Roles = RolePolicy.AuthorizeDecommission)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<DecommissionSummary>>> Create(
        [FromBody] DecommissionRequest request,
        CancellationToken cancellationToken)
    {
        var created = await decommissionService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<DecommissionSummary>.Ok(created));
    }
}
