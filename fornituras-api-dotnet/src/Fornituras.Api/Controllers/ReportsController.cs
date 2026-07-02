using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/reports")]
public sealed class ReportsController(IReportService reportService) : ControllerBase
{
    [HttpGet("totals")]
    public async Task<ActionResult<ApiResponse<ReportTotals>>> GetTotals(CancellationToken cancellationToken)
    {
        var totals = await reportService.GetTotalsAsync(cancellationToken);
        return Ok(ApiResponse<ReportTotals>.Ok(totals));
    }

    [HttpGet("active-assignments")]
    public async Task<ActionResult<ApiResponse<PageResult<ActiveAssignmentRow>>>> GetActiveAssignments(
        [FromQuery] string? qr,
        [FromQuery] string? nombre,
        [FromQuery] string? rfc,
        [FromQuery] string? placa,
        [FromQuery] string? curp,
        [FromQuery] string? municipio,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var filter = new ActiveAssignmentFilter(qr, nombre, rfc, placa, curp, municipio);
        var page = await reportService.FindActiveAssignmentsAsync(filter, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<ActiveAssignmentRow>>.Ok(page));
    }

    [HttpGet("active-assignments/export")]
    public async Task<IActionResult> ExportActiveAssignments(
        [FromQuery] string? qr,
        [FromQuery] string? nombre,
        [FromQuery] string? rfc,
        [FromQuery] string? placa,
        [FromQuery] string? curp,
        [FromQuery] string? municipio,
        CancellationToken cancellationToken)
    {
        var filter = new ActiveAssignmentFilter(qr, nombre, rfc, placa, curp, municipio);
        var bytes = await reportService.ExportActiveAssignmentsAsync(filter, cancellationToken);
        return File(bytes,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "asignaciones-activas.xlsx");
    }

    [HttpGet("predefined/{tipo}")]
    public async Task<ActionResult<ApiResponse<PageResult<EquipmentSummary>>>> GetPredefinedReport(
        PredefinedReportType tipo,
        [FromQuery] PaginationQuery pagination,
        CancellationToken cancellationToken)
    {
        var page = await reportService.FindPredefinedReportAsync(tipo, pagination, cancellationToken);
        return Ok(ApiResponse<PageResult<EquipmentSummary>>.Ok(page));
    }

    [HttpGet("predefined/{tipo}/export")]
    public async Task<IActionResult> ExportPredefinedReport(
        PredefinedReportType tipo,
        CancellationToken cancellationToken)
    {
        var bytes = await reportService.ExportPredefinedReportAsync(tipo, cancellationToken);
        return File(bytes,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            $"reporte-{tipo.ToString().ToLowerInvariant()}.xlsx");
    }
}
