package com.numobiz.solutions.fornituras.modules.qrcodes;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.CodigoQrRepository;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrCodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Caracterización de la OPACIDAD del código QR (FR-003 / SC-002).
 *
 * <p>Congela el comportamiento actual: el contenido crudo de cada código es un valor
 * opaco {@code FOR-XXXXX} sin PII ni datos derivables del elemento o de la fornitura.
 * Si un cambio futuro filtrara datos en el código, estas pruebas deben fallar.
 */
@ExtendWith(MockitoExtension.class)
class QrOpacityTest {

	// Tokens representativos de PII/datos sensibles que NUNCA deben aparecer en el código.
	private static final List<String> FORBIDDEN_TOKENS = List.of(
			"JUAN", "PEREZ", "GARCIA", "12345678", "SERIE", "NOMBRE", "CURP", "RFC", "PLACA");
	private static final Pattern OPAQUE_FORMAT = Pattern.compile("^FOR-[0-9A-Z]{5}$");

	@Mock
	private CodigoQrRepository codigoQrRepository;

	private QrCodeGeneratorService qrCodeGeneratorService;

	@BeforeEach
	void setUp() {
		QrProperties properties = new QrProperties("FOR-", 5, 10000, 100);
		qrCodeGeneratorService = new QrCodeGeneratorService(properties, codigoQrRepository);
		when(codigoQrRepository.findExistingCodigosIn(any())).thenReturn(Set.of());
	}

	@Test
	void generatedCode_isOpaqueWithNoEmbeddedData() {
		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(500);

		for (String code : codes) {
			assertTrue(OPAQUE_FORMAT.matcher(code).matches(),
					() -> "El código debe ser opaco con formato FOR-XXXXX, pero fue: " + code);
		}
	}

	@Test
	void generatedCode_containsNoPiiTokens() {
		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(500);

		for (String code : codes) {
			String suffix = code.substring("FOR-".length());
			for (String forbidden : FORBIDDEN_TOKENS) {
				assertFalse(suffix.contains(forbidden),
						() -> "El sufijo opaco no debe contener PII (" + forbidden + "): " + code);
			}
		}
	}

	@Test
	void generatedCode_usesOnlyNonSemanticCharset() {
		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(500);

		for (String code : codes) {
			String suffix = code.substring("FOR-".length());
			assertTrue(suffix.chars().allMatch(QrOpacityTest::isAllowedOpaqueChar),
					() -> "El sufijo solo debe usar [0-9A-Z] sin separadores ni minúsculas: " + code);
		}
	}

	private static boolean isAllowedOpaqueChar(int c) {
		return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z');
	}
}
