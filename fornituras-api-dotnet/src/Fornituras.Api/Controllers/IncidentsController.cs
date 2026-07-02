using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/incidents")]
public sealed class IncidentsController(IIncidentService incidentService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<IncidentSummary>>>> GetAll(
        [FromQuery] IncidentStatus? estado,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await incidentService.FindAllAsync(estado, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<IncidentSummary>>.Ok(page));
    }

    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<IncidentSummary>>> GetById(long id, CancellationToken cancellationToken)
    {
        var incident = await incidentService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<IncidentSummary>.Ok(incident));
    }

    [Authorize(Roles = RolePolicy.WriteOperations)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<IncidentSummary>>> Create(
        [FromBody] IncidentCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await incidentService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<IncidentSummary>.Ok(created));
    }

    [Authorize(Roles = RolePolicy.WriteOperations)]
    [HttpPatch("{id:long}")]
    public async Task<ActionResult<ApiResponse<IncidentSummary>>> Update(
        long id,
        [FromBody] IncidentUpdateRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await incidentService.UpdateAsync(id, request, cancellationToken);
        return Ok(ApiResponse<IncidentSummary>.Ok(updated));
    }
}
