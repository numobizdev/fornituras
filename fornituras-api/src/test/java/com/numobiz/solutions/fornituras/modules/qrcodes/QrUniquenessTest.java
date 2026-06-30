package com.numobiz.solutions.fornituras.modules.qrcodes;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrCodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caracterización de la UNICIDAD del código QR por consecutivo (FR-002 / SC-001).
 */
class QrUniquenessTest {

	private QrCodeGeneratorService qrCodeGeneratorService;

	@BeforeEach
	void setUp() {
		QrProperties properties = new QrProperties("FOR-", 6, 10000);
		qrCodeGeneratorService = new QrCodeGeneratorService(properties);
	}

	@Test
	void batch_hasNoInternalCollisions() {
		List<String> codes = qrCodeGeneratorService.formatRange(1, 1000);

		assertEquals(1000, codes.size());
		assertEquals(1000, Set.copyOf(codes).size(), "No debe haber códigos repetidos dentro del lote");
	}

	@Test
	void consecutiveRanges_doNotOverlap() {
		List<String> firstBatch = qrCodeGeneratorService.formatRange(1, 100);
		List<String> secondBatch = qrCodeGeneratorService.formatRange(101, 200);

		assertTrue(firstBatch.stream().noneMatch(secondBatch::contains),
				"Los rangos consecutivos no deben solaparse");
	}

	@Test
	void consecutiveRanges_areMonotonic() {
		List<String> codes = qrCodeGeneratorService.formatRange(50, 55);

		assertEquals(List.of("FOR-000050", "FOR-000051", "FOR-000052", "FOR-000053", "FOR-000054", "FOR-000055"),
				codes);
	}
}
