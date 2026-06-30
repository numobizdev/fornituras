package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
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
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrZipServiceTest {

	@Mock
	private QrImageService qrImageService;

	@InjectMocks
	private QrZipService qrZipService;

	@Test
	void generateZip_shouldReturnZipWithOnePngPerCode() throws Exception {
		when(qrImageService.generateCodeUnitImage(anyString(), anyDouble(), anyDouble(), eq(LabelPosition.BOTTOM),
				anyBoolean())).thenReturn(new BufferedImage(100, 120, BufferedImage.TYPE_INT_RGB));

		LoteQR lote = new LoteQR();
		lote.setId(7L);
		lote.setQrSizeCm(new BigDecimal("3.0"));
		lote.setPaddingCm(new BigDecimal("0.5"));
		lote.setLabelPosition(LabelPosition.BOTTOM);
		lote.setMostrarBordes(true);

		CodigoQR codigo1 = new CodigoQR();
		codigo1.setCodigo("FOR-ABC12");
		CodigoQR codigo2 = new CodigoQR();
		codigo2.setCodigo("FOR-XYZ99");

		byte[] zipBytes = qrZipService.generateZip(lote, List.of(codigo1, codigo2));

		assertNotNull(zipBytes);
		assertTrue(zipBytes.length > 0);

		int entries = 0;
		try (ZipInputStream zipInputStream = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				entries++;
				assertTrue(entry.getName().endsWith(".png"));
			}
		}
		assertEquals(2, entries);
	}
}
