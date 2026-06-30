package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrPdfServiceTest {

	@Mock
	private QrImageService qrImageService;

	@InjectMocks
	private QrPdfService qrPdfService;

	@Test
	void generatePdf_shouldReturnNonEmptyBytes() {
		when(qrImageService.generateStickerImage(anyString(), anyDouble(), anyDouble(), anyBoolean()))
				.thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));

		LoteQR lote = new LoteQR();
		lote.setConsecutivoInicial(1);
		lote.setConsecutivoFinal(2);
		lote.setDescripcion("Códigos prendas Chiapas");
		lote.setCantidad(2);
		lote.setQrSizeCm(new BigDecimal("3.0"));
		lote.setPaddingCm(new BigDecimal("0.5"));
		lote.setLabelPosition(LabelPosition.BOTTOM);
		lote.setMostrarBordes(true);

		byte[] pdf = qrPdfService.generatePdf(lote, List.of("FOR-000001", "FOR-000002"));

		assertNotNull(pdf);
		assertTrue(pdf.length > 0);
	}
}
