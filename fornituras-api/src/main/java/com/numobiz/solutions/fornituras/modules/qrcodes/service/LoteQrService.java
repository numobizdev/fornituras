package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.GenerateQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.LoteQrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoteQrService {

	private static final Logger log = LoggerFactory.getLogger(LoteQrService.class);

	private final LoteQrRepository loteQrRepository;
	private final QrCodeGeneratorService qrCodeGeneratorService;
	private final QrProperties qrProperties;

	public LoteQrService(
			LoteQrRepository loteQrRepository,
			QrCodeGeneratorService qrCodeGeneratorService,
			QrProperties qrProperties) {
		this.loteQrRepository = loteQrRepository;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.qrProperties = qrProperties;
	}

	@Transactional
	public LoteQR generate(GenerateQrForm form) {
		if (form.cantidad() > qrProperties.maxBatchSize()) {
			throw new BadRequestException("Maximum batch size is " + qrProperties.maxBatchSize());
		}

		log.info("Generating QR lote with {} codes", form.cantidad());
		long totalStart = System.nanoTime();

		int consecutivoInicial = loteQrRepository.findMaxConsecutivoFinalForUpdate() + 1;
		int consecutivoFinal = consecutivoInicial + form.cantidad() - 1;
		if (consecutivoFinal > qrCodeGeneratorService.maxConsecutivo()) {
			throw new BadRequestException("Maximum consecutive number exceeded. Reduce batch size.");
		}

		LoteQR lote = new LoteQR();
		lote.setConsecutivoInicial(consecutivoInicial);
		lote.setConsecutivoFinal(consecutivoFinal);
		lote.setDescripcion(form.descripcion().trim());
		lote.setCantidad(form.cantidad());
		lote.setQrSizeCm(form.qrSizeCm());
		lote.setPaddingCm(form.paddingCm());
		lote.setLabelPosition(form.labelPosition());
		lote.setMostrarBordes(form.mostrarBordes());
		lote = loteQrRepository.save(lote);

		long totalDurationMs = (System.nanoTime() - totalStart) / 1_000_000;
		log.info(
				"QR lote {} generated in {} ms ({} codes, FOR-{}..FOR-{})",
				lote.getId(),
				totalDurationMs,
				form.cantidad(),
				String.format("%0" + qrProperties.sequenceLength() + "d", consecutivoInicial),
				String.format("%0" + qrProperties.sequenceLength() + "d", consecutivoFinal));
		return lote;
	}

	@Transactional(readOnly = true)
	public LoteQR findById(Long id) {
		return loteQrRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("QR lote not found with id: " + id));
	}

	@Transactional(readOnly = true)
	public List<LoteQR> findAll() {
		return loteQrRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public List<String> listCodigos(Long loteId) {
		LoteQR lote = findById(loteId);
		return qrCodeGeneratorService.formatRange(lote.getConsecutivoInicial(), lote.getConsecutivoFinal());
	}
}
