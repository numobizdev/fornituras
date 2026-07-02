using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/assignments")]
public sealed class AssignmentsController(IAssignmentService assignmentService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<AssignmentSummary>>>> GetAll(
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await assignmentService.FindAllAsync(pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<AssignmentSummary>>.Ok(page));
    }

    [Authorize(Roles = RolePolicy.WriteOperations)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<AssignmentSummary>>> Assign(
        [FromBody] AssignRequest request,
        CancellationToken cancellationToken)
    {
        var assignment = await assignmentService.AssignAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<AssignmentSummary>.Ok(assignment));
    }

    [Authorize(Roles = RolePolicy.WriteOperations)]
    [HttpPost("{id:long}/return")]
    public async Task<ActionResult<ApiResponse<AssignmentSummary>>> Return(
        long id,
        CancellationToken cancellationToken)
    {
        var assignment = await assignmentService.ReturnAsync(id, cancellationToken);
        return Ok(ApiResponse<AssignmentSummary>.Ok(assignment));
    }

    [Authorize(Roles = RolePolicy.WriteOperations)]
    [HttpPost("reassign")]
    public async Task<ActionResult<ApiResponse<AssignmentSummary>>> Reassign(
        [FromBody] ReassignRequest request,
        CancellationToken cancellationToken)
    {
        var assignment = await assignmentService.ReassignAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<AssignmentSummary>.Ok(assignment));
    }
}
