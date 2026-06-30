package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class QrZipService {

	private static final Logger log = LoggerFactory.getLogger(QrZipService.class);

	private final QrImageService qrImageService;

	public QrZipService(QrImageService qrImageService) {
		this.qrImageService = qrImageService;
	}

	public byte[] generateZip(LoteQR lote, List<CodigoQR> codigos) {
		return generateZip(lote, codigos, lote.getQrSizeCm(), lote.getPaddingCm(), lote.getLabelPosition(),
				lote.isMostrarBordes());
	}

	public byte[] generateZip(LoteQR lote, List<CodigoQR> codigos, BigDecimal qrSizeCm, BigDecimal paddingCm,
			LabelPosition labelPosition, boolean mostrarBordes) {
		if (codigos.isEmpty()) {
			throw new BadRequestException("No QR codes provided for ZIP generation");
		}

		long start = System.nanoTime();
		double qrSize = toDouble(qrSizeCm);
		double padding = toDouble(paddingCm);
		Set<String> usedNames = new HashSet<>();

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(outputStream)) {
			for (CodigoQR codigoQR : codigos) {
				String codigo = codigoQR.getCodigo();
				BufferedImage image = qrImageService.generateCodeUnitImage(codigo, qrSize, padding, labelPosition,
						mostrarBordes);
				String entryName = uniqueEntryName(codigo, usedNames);

				zip.putNextEntry(new ZipEntry(entryName));
				ImageIO.write(image, "PNG", zip);
				zip.closeEntry();
			}
			zip.finish();

			byte[] zipBytes = outputStream.toByteArray();
			long durationMs = (System.nanoTime() - start) / 1_000_000;
			log.info("QR ZIP generated for lote {} in {} ms ({} codes, {} KB)",
					lote.getId(), durationMs, codigos.size(), zipBytes.length / 1024);
			return zipBytes;
		} catch (Exception ex) {
			throw new BadRequestException("Unable to generate QR ZIP");
		}
	}

	private String uniqueEntryName(String codigo, Set<String> usedNames) {
		String baseName = sanitizeFileName(codigo);
		String entryName = baseName + ".png";
		int suffix = 2;
		while (!usedNames.add(entryName)) {
			entryName = baseName + "-" + suffix + ".png";
			suffix++;
		}
		return entryName;
	}

	private String sanitizeFileName(String codigo) {
		String sanitized = codigo.replaceAll("[^a-zA-Z0-9._-]", "_");
		return sanitized.isEmpty() ? "codigo" : sanitized;
	}

	private double toDouble(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
