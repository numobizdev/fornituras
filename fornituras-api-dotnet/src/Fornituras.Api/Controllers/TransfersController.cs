using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/transfers")]
public sealed class TransfersController(ITransferService transferService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<TransferSummary>>>> GetAll(
        [FromQuery] long? origenId,
        [FromQuery] long? destinoId,
        [FromQuery] TransferStatus? status,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await transferService.FindAllAsync(origenId, destinoId, status, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<TransferSummary>>.Ok(page));
    }

    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<TransferDetail>>> GetById(long id, CancellationToken cancellationToken)
    {
        var transfer = await transferService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<TransferDetail>.Ok(transfer));
    }

    [Authorize(Roles = RolePolicy.WriteTransfers)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<TransferDetail>>> Create(
        [FromBody] TransferCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await transferService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<TransferDetail>.Ok(created));
    }

    [Authorize(Roles = RolePolicy.WriteTransfers)]
    [HttpPost("{id:long}/receive")]
    public async Task<ActionResult<ApiResponse<TransferDetail>>> Receive(long id, CancellationToken cancellationToken)
    {
        var transfer = await transferService.ReceiveAsync(id, cancellationToken);
        return Ok(ApiResponse<TransferDetail>.Ok(transfer));
    }

    [Authorize(Roles = RolePolicy.WriteTransfers)]
    [HttpPost("{id:long}/cancel")]
    public async Task<ActionResult<ApiResponse<TransferDetail>>> Cancel(long id, CancellationToken cancellationToken)
    {
        var transfer = await transferService.CancelAsync(id, cancellationToken);
        return Ok(ApiResponse<TransferDetail>.Ok(transfer));
    }
}
