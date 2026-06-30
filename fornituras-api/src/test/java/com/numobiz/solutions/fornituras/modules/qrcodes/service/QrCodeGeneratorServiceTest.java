package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.CodigoQrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeGeneratorServiceTest {

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
	void generateUniqueCode_shouldMatchFormat() {
		String code = qrCodeGeneratorService.generateUniqueCode(new HashSet<>());
		assertTrue(qrCodeGeneratorService.isValidFormat(code));
		assertTrue(code.startsWith("FOR-"));
		assertEquals(9, code.length());
	}

	@Test
	void generateUniqueCode_shouldNotContainEnye() {
		Set<String> batch = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			String code = qrCodeGeneratorService.generateUniqueCode(batch);
			assertFalse(code.contains("Ñ"));
			batch.add(code);
		}
	}

	@Test
	void generateUniqueCode_shouldNotRepeatWithinBatch() {
		Set<String> batch = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			batch.add(qrCodeGeneratorService.generateUniqueCode(batch));
		}
		assertEquals(20, batch.size());
	}

	@Test
	void generateUniqueCodes_shouldGenerateRequestedAmountInBatch() {
		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(250);

		assertEquals(250, codes.size());
		assertEquals(250, Set.copyOf(codes).size());
		codes.forEach(code -> assertTrue(qrCodeGeneratorService.isValidFormat(code)));
	}

	@Test
	void generateUniqueCodes_shouldRejectCodesAlreadyInDatabase() {
		when(codigoQrRepository.findExistingCodigosIn(any())).thenAnswer(invocation -> {
			Collection<String> candidates = invocation.getArgument(0);
			return candidates.stream().limit(1).collect(java.util.stream.Collectors.toSet());
		});

		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(10);

		assertEquals(10, codes.size());
		assertEquals(10, Set.copyOf(codes).size());
	}
}
