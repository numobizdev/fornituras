package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.dto.GenerateQrForm;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.CodigoQrRepository;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.LoteQrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoteQrService {

	private static final Logger log = LoggerFactory.getLogger(LoteQrService.class);

	private final LoteQrRepository loteQrRepository;
	private final CodigoQrRepository codigoQrRepository;
	private final QrCodeGeneratorService qrCodeGeneratorService;
	private final QrProperties qrProperties;

	public LoteQrService(
			LoteQrRepository loteQrRepository,
			CodigoQrRepository codigoQrRepository,
			QrCodeGeneratorService qrCodeGeneratorService,
			QrProperties qrProperties) {
		this.loteQrRepository = loteQrRepository;
		this.codigoQrRepository = codigoQrRepository;
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

		LoteQR lote = new LoteQR();
		lote.setDescripcion(form.descripcion().trim());
		lote.setCantidad(form.cantidad());
		lote.setQrSizeCm(form.qrSizeCm());
		lote.setPaddingCm(form.paddingCm());
		lote.setLabelPosition(form.labelPosition());
		lote.setMostrarBordes(form.mostrarBordes());
		lote = loteQrRepository.save(lote);

		long codesStart = System.nanoTime();
		List<String> codigosGenerados = qrCodeGeneratorService.generateUniqueCodes(form.cantidad());
		long codesDurationMs = (System.nanoTime() - codesStart) / 1_000_000;
		log.info("QR codes generated in {} ms ({} codes)", codesDurationMs, codigosGenerados.size());

		List<CodigoQR> codigos = new ArrayList<>(codigosGenerados.size());

		for (String codigo : codigosGenerados) {
			CodigoQR codigoQR = new CodigoQR();
			codigoQR.setCodigo(codigo);
			codigoQR.setLoteQr(lote);
			codigos.add(codigoQR);
		}

		long persistStart = System.nanoTime();
		codigoQrRepository.saveAll(codigos);
		long persistDurationMs = (System.nanoTime() - persistStart) / 1_000_000;
		long totalDurationMs = (System.nanoTime() - totalStart) / 1_000_000;
		log.info("QR codes persisted in {} ms", persistDurationMs);
		log.info("QR lote {} generated in {} ms ({} codes)", lote.getId(), totalDurationMs, codigos.size());
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
	public List<CodigoQR> listCodigos(Long loteId) {
		findById(loteId);
		return codigoQrRepository.findByLoteQrIdOrderByCodigoAsc(loteId);
	}
}
