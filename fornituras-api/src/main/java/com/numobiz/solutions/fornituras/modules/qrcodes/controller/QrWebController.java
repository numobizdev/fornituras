package com.numobiz.solutions.fornituras.modules.qrcodes.controller;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.GenerateQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.ReprintQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.LoteQrService;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/qr")
public class QrWebController {

	private final LoteQrService loteQrService;
	private final QrPdfService qrPdfService;
	private final QrProperties qrProperties;

	public QrWebController(LoteQrService loteQrService, QrPdfService qrPdfService, QrProperties qrProperties) {
		this.loteQrService = loteQrService;
		this.qrPdfService = qrPdfService;
		this.qrProperties = qrProperties;
	}

	@ModelAttribute("maxBatchSize")
	public int maxBatchSize() {
		return qrProperties.maxBatchSize();
	}

	@GetMapping("/generar")
	public String showForm(Model model) {
		model.addAttribute("form", GenerateQrForm.defaults());
		return "qr/generar";
	}

	@PostMapping("/generar")
	public String generate(@Valid @ModelAttribute("form") GenerateQrForm form, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "qr/generar";
		}

		LoteQR lote = loteQrService.generate(form);
		return "redirect:/qr/lotes/" + lote.getId() + "/exito";
	}

	@GetMapping("/lotes")
	public String listLotes(Model model) {
		model.addAttribute("lotes", loteQrService.findAll());
		return "qr/lotes";
	}

	@GetMapping("/lotes/{id}")
	public String showLote(@PathVariable Long id, Model model) {
		LoteQR lote = loteQrService.findById(id);
		model.addAttribute("lote", lote);
		model.addAttribute("form", ReprintQrForm.from(lote));
		return "qr/lote-detalle";
	}

	@GetMapping("/lotes/{id}/exito")
	public String success(@PathVariable Long id, Model model) {
		LoteQR lote = loteQrService.findById(id);
		model.addAttribute("lote", lote);
		return "qr/exito";
	}

	@GetMapping("/lotes/{id}/pdf")
	public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
		LoteQR lote = loteQrService.findById(id);
		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] pdf = qrPdfService.generatePdf(lote, codigos);
		return pdfResponse(id, pdf);
	}

	@PostMapping("/lotes/{id}/reimprimir")
	public Object reprintPdf(
			@PathVariable Long id,
			@Valid @ModelAttribute("form") ReprintQrForm form,
			BindingResult bindingResult,
			Model model) {
		LoteQR lote = loteQrService.findById(id);
		if (bindingResult.hasErrors()) {
			model.addAttribute("lote", lote);
			return "qr/lote-detalle";
		}

		List<CodigoQR> codigos = loteQrService.listCodigos(id);
		byte[] pdf = qrPdfService.generatePdf(lote, codigos, form.qrSizeCm(), form.paddingCm(), form.labelPosition(),
				form.mostrarBordes());
		return pdfResponse(id, pdf);
	}

	private ResponseEntity<byte[]> pdfResponse(Long id, byte[] pdf) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lote-qr-" + id + ".pdf")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}
}
