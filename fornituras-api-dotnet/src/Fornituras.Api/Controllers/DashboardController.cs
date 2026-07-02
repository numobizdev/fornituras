using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/dashboard")]
public sealed class DashboardController(IDashboardService dashboardService) : ControllerBase
{
    [HttpGet("summary")]
    public async Task<ActionResult<ApiResponse<DashboardSummary>>> GetSummary(CancellationToken cancellationToken)
    {
        var summary = await dashboardService.GetSummaryAsync(cancellationToken);
        return Ok(ApiResponse<DashboardSummary>.Ok(summary));
    }
}
