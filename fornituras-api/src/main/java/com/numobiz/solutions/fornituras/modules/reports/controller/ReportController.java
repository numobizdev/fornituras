package com.numobiz.solutions.fornituras.modules.reports.controller;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.reports.dto.ActiveAssignmentFilter;
import com.numobiz.solutions.fornituras.modules.reports.dto.ActiveAssignmentRow;
import com.numobiz.solutions.fornituras.modules.reports.dto.PredefinedReportType;
import com.numobiz.solutions.fornituras.modules.reports.dto.ReportTotals;
import com.numobiz.solutions.fornituras.modules.reports.service.ExcelExportService;
import com.numobiz.solutions.fornituras.modules.reports.service.PredefinedReportService;
import com.numobiz.solutions.fornituras.modules.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * API de reportes y estadística (011). Vista de control: totales por estado (coinciden con 010),
 * asignaciones activas con filtros y exportación a Excel. Requiere autenticación; la <b>PII se
 * enmascara por rol</b> (solo ADMIN ve CURP/RFC en claro, en pantalla y en el archivo). Toda
 * exportación queda <b>auditada</b> sin PII en el registro (FR-005).
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reportes y estadística (vista de control, export a Excel)")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ReportService reportService;
	private final PredefinedReportService predefinedReportService;
	private final ExcelExportService excelExportService;
	private final AuditWriter audit;

	public ReportController(
			ReportService reportService,
			PredefinedReportService predefinedReportService,
			ExcelExportService excelExportService,
			AuditWriter audit) {
		this.reportService = reportService;
		this.predefinedReportService = predefinedReportService;
		this.excelExportService = excelExportService;
		this.audit = audit;
	}

	@GetMapping("/totals")
	@Operation(summary = "Totales por estado", description = "Totales de fornituras por estado (coinciden con el tablero 010) y conteo de elementos. Sin PII.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<ReportTotals>> totals() {
		return ResponseEntity.ok(ApiResponse.ok(reportService.totals()));
	}

	@GetMapping("/active-assignments")
	@Operation(summary = "Asignaciones activas", description = "Asignaciones vigentes, paginadas y filtrables (QR, nombre, RFC, placa, CURP, municipio). CURP/RFC enmascaradas salvo para ADMIN.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<ActiveAssignmentRow>>> activeAssignments(
			@RequestParam(required = false) String qr,
			@RequestParam(required = false) String nombre,
			@RequestParam(required = false) String rfc,
			@RequestParam(required = false) String placa,
			@RequestParam(required = false) String curp,
			@RequestParam(required = false) String municipio,
			Pageable pageable) {
		ActiveAssignmentFilter filter = new ActiveAssignmentFilter(qr, nombre, rfc, placa, curp, municipio);
		return ResponseEntity.ok(ApiResponse.ok(reportService.activeAssignments(filter, pageable)));
	}

	@GetMapping("/active-assignments/export")
	@Operation(summary = "Exportar asignaciones activas (Excel)", description = "Descarga en .xlsx las asignaciones activas del filtro, con PII enmascarada según rol. La exportación se audita.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<StreamingResponseBody> exportActiveAssignments(
			@RequestParam(required = false) String qr,
			@RequestParam(required = false) String nombre,
			@RequestParam(required = false) String rfc,
			@RequestParam(required = false) String placa,
			@RequestParam(required = false) String curp,
			@RequestParam(required = false) String municipio) {
		ActiveAssignmentFilter filter = new ActiveAssignmentFilter(qr, nombre, rfc, placa, curp, municipio);
		List<ActiveAssignmentRow> rows = reportService.activeAssignmentRows(filter);
		audit.recordEvent("EXPORT_REPORT", "report=ACTIVE_ASSIGNMENTS filters=" + usedFilters(filter)
				+ " rows=" + rows.size() + " piiMasked=" + reportService.piiMaskedForCurrentActor());

		StreamingResponseBody body = out -> excelExportService.writeActiveAssignments(rows, out);
		return download(body, "asignaciones-activas.xlsx");
	}

	@GetMapping("/predefined/{tipo}")
	@Operation(summary = "Reporte predefinido", description = "Reporte operativo por tipo (inventario general, disponibles, asignadas, mantenimiento, baja). Paginado. Sin PII.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<EquipmentSummary>>> predefined(
			@PathVariable PredefinedReportType tipo, Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(predefinedReportService.report(tipo, pageable)));
	}

	@GetMapping("/predefined/{tipo}/export")
	@Operation(summary = "Exportar reporte predefinido (Excel)", description = "Descarga en .xlsx el reporte predefinido. La exportación se audita.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<StreamingResponseBody> exportPredefined(@PathVariable PredefinedReportType tipo) {
		List<EquipmentSummary> rows = predefinedReportService.reportRows(tipo);
		audit.recordEvent("EXPORT_REPORT", "report=PREDEFINED:" + tipo.name() + " rows=" + rows.size());

		StreamingResponseBody body = out -> excelExportService.writeEquipment(rows, out);
		return download(body, "reporte-" + tipo.name().toLowerCase() + ".xlsx");
	}

	private ResponseEntity<StreamingResponseBody> download(StreamingResponseBody body, String filename) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType(XLSX))
				.body(body);
	}

	/** Lista los nombres de los filtros usados (nunca sus valores) para auditar sin PII. */
	private String usedFilters(ActiveAssignmentFilter filter) {
		List<String> used = new ArrayList<>();
		if (notBlank(filter.qr())) used.add("qr");
		if (notBlank(filter.nombre())) used.add("nombre");
		if (notBlank(filter.rfc())) used.add("rfc");
		if (notBlank(filter.placa())) used.add("placa");
		if (notBlank(filter.curp())) used.add("curp");
		if (notBlank(filter.municipio())) used.add("municipio");
		return used.toString();
	}

	private boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
