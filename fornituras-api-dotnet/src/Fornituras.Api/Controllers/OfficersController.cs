using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/officers")]
public sealed class OfficersController(IOfficerService officerService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<OfficerSummary>>>> GetAll(
        [FromQuery] string? q,
        [FromQuery] string? municipio,
        [FromQuery] long? sexoId,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await officerService.FindAllAsync(q, municipio, sexoId, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<OfficerSummary>>.Ok(page));
    }

    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<OfficerDetail>>> GetById(long id, CancellationToken cancellationToken)
    {
        var officer = await officerService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<OfficerDetail>.Ok(officer));
    }

    [Authorize(Roles = RolePolicy.WriteOfficers)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<OfficerDetail>>> Create(
        [FromBody] OfficerCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await officerService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<OfficerDetail>.Ok(created));
    }
}
