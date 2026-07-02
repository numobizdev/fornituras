using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize(Roles = RolePolicy.ReadAudit)]
[Route("api/v1/audit")]
public sealed class AuditController(IAuditService auditService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<AuditLogSummary>>>> GetAll(
        [FromQuery] string? actor,
        [FromQuery] string? accion,
        [FromQuery] string? entidad,
        [FromQuery] DateTime? desde,
        [FromQuery] DateTime? hasta,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await auditService.FindAllAsync(actor, accion, entidad, desde, hasta, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<AuditLogSummary>>.Ok(page));
    }
}
