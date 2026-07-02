using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/alerts")]
public sealed class AlertsController(IAlertService alertService) : ControllerBase
{
    [HttpGet("vigencia")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<AlertItem>>>> GetVigenciaAlerts(
        CancellationToken cancellationToken)
    {
        var alerts = await alertService.FindVigenciaAlertsAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<AlertItem>>.Ok(alerts));
    }
}
