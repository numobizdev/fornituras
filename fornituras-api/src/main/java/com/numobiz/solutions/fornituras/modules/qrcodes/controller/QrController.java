package com.numobiz.solutions.fornituras.modules.qrcodes.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.CodigoQrResponseDTO;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.GenerateQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.LoteQrResponseDTO;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.ReprintQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.LoteQrService;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrPdfService;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrZipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qr")
@Tag(name = "QR Codes", description = "QR code batch generation and export")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
public class QrController {

	private final LoteQrService loteQrService;
	private final QrPdfService qrPdfService;
	private final QrZipService qrZipService;

	public QrController(LoteQrService loteQrService, QrPdfService qrPdfService, QrZipService qrZipService) {
		this.loteQrService = loteQrService;
		this.qrPdfService = qrPdfService;
		this.qrZipService = qrZipService;
	}

	@PostMapping("/lotes")
	@Operation(summary = "Generate QR batch", description = "Creates a new batch of unique QR codes with the given print settings")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Batch created")
	public ResponseEntity<ApiResponse<LoteQrResponseDTO>> generate(@Valid @RequestBody GenerateQrForm form) {
		LoteQR lote = loteQrService.generate(form);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Lote QR generado correctamente", LoteQrResponseDTO.from(lote)));
	}

	@GetMapping("/lotes")
	@Operation(summary = "List QR batches", description = "Returns all QR batches ordered by creation date (newest first)")
	public ResponseEntity<ApiResponse<List<LoteQrResponseDTO>>> listLotes() {
		List<LoteQrResponseDTO> lotes = loteQrService.findAll().stream()
				.map(LoteQrResponseDTO::from)
				.toList();
		return ResponseEntity.ok(ApiResponse.ok(lotes));
	}

	@GetMapping("/lotes/{id}")
	@Operation(summary = "Get QR batch", description = "Returns details for a single QR batch")
	public ResponseEntity<ApiResponse<LoteQrResponseDTO>> getLote(@PathVariable Long id) {
		LoteQR lote = loteQrService.findById(id);
		return ResponseEntity.ok(ApiResponse.ok(LoteQrResponseDTO.from(lote)));
	}

	@GetMapping("/lotes/{id}/codigos")
	@Operation(summary = "List codes in batch", description = "Returns all QR codes belonging to a batch")
	public ResponseEntity<ApiResponse<List<CodigoQrResponseDTO>>> listCodigos(@PathVariable Long id) {
		List<CodigoQrResponseDTO> codigos = loteQrService.listCodigos(id).stream()
				.map(codigo -> CodigoQrResponseDTO.from(codigo, id))
				.toList();
		return ResponseEntity.ok(ApiResponse.ok(codigos));
	}

	@GetMapping("/lotes/{id}/pdf")
	@Operation(summary = "Download PDF", description = "Downloads a printable PDF for the batch using its original settings")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF file",
			content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))))
	public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
		LoteQR lote = loteQrService.findById(id);
		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] pdf = qrPdfService.generatePdf(lote, codigos);
		return fileResponse(id, pdf, "pdf", MediaType.APPLICATION_PDF);
	}

	@GetMapping("/lotes/{id}/zip")
	@Operation(summary = "Download ZIP (PNG)", description = "Downloads a ZIP archive with one PNG per code using the batch original settings")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ZIP file",
			content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary"))))
	public ResponseEntity<byte[]> downloadZip(@PathVariable Long id) {
		LoteQR lote = loteQrService.findById(id);
		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] zip = qrZipService.generateZip(lote, codigos);
		return fileResponse(id, zip, "zip", MediaType.parseMediaType("application/zip"));
	}

	@PostMapping("/lotes/{id}/export/pdf")
	@Operation(summary = "Export PDF with custom settings",
			description = "Generates a PDF using custom size, padding, label, and border options. The codes in the batch do not change.")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF file",
			content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))))
	public ResponseEntity<byte[]> exportPdf(@PathVariable Long id, @Valid @RequestBody ReprintQrForm form) {
		LoteQR lote = loteQrService.findById(id);
		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] pdf = qrPdfService.generatePdf(lote, codigos, form.qrSizeCm(), form.paddingCm(), form.labelPosition(),
				form.mostrarBordes());
		return fileResponse(id, pdf, "pdf", MediaType.APPLICATION_PDF);
	}

	@PostMapping("/lotes/{id}/export/zip")
	@Operation(summary = "Export ZIP (PNG) with custom settings",
			description = "Generates a ZIP with one PNG per code using custom size, padding, label, and border options")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ZIP file",
			content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary"))))
	public ResponseEntity<byte[]> exportZip(@PathVariable Long id, @Valid @RequestBody ReprintQrForm form) {
		LoteQR lote = loteQrService.findById(id);
		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] zip = qrZipService.generateZip(lote, codigos, form.qrSizeCm(), form.paddingCm(), form.labelPosition(),
				form.mostrarBordes());
		return fileResponse(id, zip, "zip", MediaType.parseMediaType("application/zip"));
	}

	private ResponseEntity<byte[]> fileResponse(Long id, byte[] content, String extension, MediaType mediaType) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lote-qr-" + id + "." + extension)
				.contentType(mediaType)
				.body(content);
	}
}
