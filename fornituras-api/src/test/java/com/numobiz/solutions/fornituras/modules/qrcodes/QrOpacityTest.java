package com.numobiz.solutions.fornituras.modules.qrcodes;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrCodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caracterización de la OPACIDAD del código QR (FR-003 / SC-002).
 */
class QrOpacityTest {

	private static final List<String> FORBIDDEN_TOKENS = List.of(
			"JUAN", "PEREZ", "GARCIA", "12345678", "SERIE", "NOMBRE", "CURP", "RFC", "PLACA");
	private static final Pattern OPAQUE_FORMAT = Pattern.compile("^FOR-\\d{6}$");

	private QrCodeGeneratorService qrCodeGeneratorService;

	@BeforeEach
	void setUp() {
		QrProperties properties = new QrProperties("FOR-", 6, 10000);
		qrCodeGeneratorService = new QrCodeGeneratorService(properties);
	}

	@Test
	void generatedCode_isOpaqueWithNoEmbeddedData() {
		List<String> codes = qrCodeGeneratorService.formatRange(1, 500);

		for (String code : codes) {
			assertTrue(OPAQUE_FORMAT.matcher(code).matches(),
					() -> "El código debe ser opaco con formato FOR-000001, pero fue: " + code);
		}
	}

	@Test
	void generatedCode_containsNoPiiTokens() {
		List<String> codes = qrCodeGeneratorService.formatRange(1, 500);

		for (String code : codes) {
			String suffix = code.substring("FOR-".length());
			for (String forbidden : FORBIDDEN_TOKENS) {
				assertFalse(suffix.contains(forbidden),
						() -> "El sufijo opaco no debe contener PII (" + forbidden + "): " + code);
			}
		}
	}

	@Test
	void generatedCode_usesOnlyNumericSequence() {
		List<String> codes = qrCodeGeneratorService.formatRange(1, 500);

		for (String code : codes) {
			String suffix = code.substring("FOR-".length());
			assertTrue(suffix.chars().allMatch(c -> c >= '0' && c <= '9'),
					() -> "El sufijo solo debe usar dígitos: " + code);
		}
	}
}
