using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize(Roles = RolePolicy.WriteInventory)]
[Route("api/v1/qr")]
public sealed class QrController(IQrService qrService) : ControllerBase
{
    [HttpPost("lotes")]
    public async Task<ActionResult<ApiResponse<LoteQrResponse>>> GenerateLote(
        [FromBody] GenerateQrForm form,
        CancellationToken cancellationToken)
    {
        var lote = await qrService.GenerateLoteAsync(form, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<LoteQrResponse>.Ok(lote));
    }

    [HttpGet("lotes")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<LoteQrResponse>>>> GetLotes(
        CancellationToken cancellationToken)
    {
        var lotes = await qrService.FindLotesAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<LoteQrResponse>>.Ok(lotes));
    }

    [HttpGet("lotes/{id:long}")]
    public async Task<ActionResult<ApiResponse<LoteQrResponse>>> GetLoteById(
        long id,
        CancellationToken cancellationToken)
    {
        var lote = await qrService.FindLoteByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<LoteQrResponse>.Ok(lote));
    }

    [HttpGet("lotes/{id:long}/codigos")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<CodigoQrResponse>>>> GetCodigos(
        long id,
        CancellationToken cancellationToken)
    {
        var codigos = await qrService.FindCodigosAsync(id, cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<CodigoQrResponse>>.Ok(codigos));
    }

    [HttpGet("lotes/{id:long}/pdf")]
    public async Task<IActionResult> ExportPdf(long id, CancellationToken cancellationToken)
    {
        var bytes = await qrService.ExportPdfAsync(id, cancellationToken);
        return File(bytes, "application/pdf", $"lote-{id}.pdf");
    }

    [HttpGet("lotes/{id:long}/zip")]
    public async Task<IActionResult> ExportZip(long id, CancellationToken cancellationToken)
    {
        var bytes = await qrService.ExportZipAsync(id, cancellationToken);
        return File(bytes, "application/zip", $"lote-{id}.zip");
    }

    [HttpPost("lotes/{id:long}/export/pdf")]
    public async Task<IActionResult> ReprintPdf(
        long id,
        [FromBody] ReprintQrForm form,
        CancellationToken cancellationToken)
    {
        var bytes = await qrService.ReprintPdfAsync(id, form, cancellationToken);
        return File(bytes, "application/pdf", $"lote-{id}-reprint.pdf");
    }

    [HttpPost("lotes/{id:long}/export/zip")]
    public async Task<IActionResult> ReprintZip(
        long id,
        [FromBody] ReprintQrForm form,
        CancellationToken cancellationToken)
    {
        var bytes = await qrService.ReprintZipAsync(id, form, cancellationToken);
        return File(bytes, "application/zip", $"lote-{id}-reprint.zip");
    }
}
