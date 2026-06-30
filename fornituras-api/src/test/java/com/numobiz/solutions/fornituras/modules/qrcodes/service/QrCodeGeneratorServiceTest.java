package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.config.QrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeGeneratorServiceTest {

	private QrCodeGeneratorService qrCodeGeneratorService;

	@BeforeEach
	void setUp() {
		QrProperties properties = new QrProperties("FOR-", 6, 10000);
		qrCodeGeneratorService = new QrCodeGeneratorService(properties);
	}

	@Test
	void formatCode_shouldMatchSequentialFormat() {
		assertEquals("FOR-000001", qrCodeGeneratorService.formatCode(1));
		assertEquals("FOR-000042", qrCodeGeneratorService.formatCode(42));
		assertTrue(qrCodeGeneratorService.isValidFormat("FOR-000001"));
	}

	@Test
	void formatRange_shouldReturnSequentialCodes() {
		List<String> codes = qrCodeGeneratorService.formatRange(1, 5);

		assertEquals(5, codes.size());
		assertEquals(List.of("FOR-000001", "FOR-000002", "FOR-000003", "FOR-000004", "FOR-000005"), codes);
	}

	@Test
	void formatRange_shouldRejectInvalidRange() {
		assertThrows(BadRequestException.class, () -> qrCodeGeneratorService.formatRange(10, 5));
	}

	@Test
	void formatCode_shouldRejectOutOfRangeValues() {
		assertThrows(BadRequestException.class, () -> qrCodeGeneratorService.formatCode(0));
		assertThrows(BadRequestException.class, () -> qrCodeGeneratorService.formatCode(1_000_000));
	}

	@Test
	void isValidFormat_shouldRejectLegacyRandomCodes() {
		assertFalse(qrCodeGeneratorService.isValidFormat("FOR-ABC12"));
		assertFalse(qrCodeGeneratorService.isValidFormat("FOR-00001"));
	}

	@Test
	void maxConsecutivo_shouldMatchSequenceLength() {
		assertEquals(999_999, qrCodeGeneratorService.maxConsecutivo());
	}
}
