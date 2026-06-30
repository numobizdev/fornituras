package com.numobiz.solutions.fornituras.modules.qrcodes;

import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.CodigoQrRepository;
import com.numobiz.solutions.fornituras.modules.qrcodes.service.QrCodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Caracterización de la UNICIDAD del código QR (FR-002 / SC-001).
 *
 * <p>Congela las tres garantías del generador actual: cero colisiones dentro del lote,
 * cero colisiones contra los códigos ya existentes en BD y cero colisiones contra los
 * códigos reservados del lote en curso.
 */
@ExtendWith(MockitoExtension.class)
class QrUniquenessTest {

	@Mock
	private CodigoQrRepository codigoQrRepository;

	private QrCodeGeneratorService qrCodeGeneratorService;

	@BeforeEach
	void setUp() {
		QrProperties properties = new QrProperties("FOR-", 5, 10000, 100);
		qrCodeGeneratorService = new QrCodeGeneratorService(properties, codigoQrRepository);
	}

	@Test
	void batch_hasNoInternalCollisions() {
		when(codigoQrRepository.findExistingCodigosIn(any())).thenReturn(Set.of());

		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(1000);

		assertEquals(1000, codes.size());
		assertEquals(1000, Set.copyOf(codes).size(), "No debe haber códigos repetidos dentro del lote");
	}

	@Test
	void batch_neverReusesCodesAlreadyInDatabase() {
		Set<String> alreadyInDatabase = new HashSet<>();
		when(codigoQrRepository.findExistingCodigosIn(any())).thenAnswer(invocation -> {
			Collection<String> candidates = invocation.getArgument(0);
			// Simula que el primer candidato de cada lote ya existe en BD.
			Set<String> existing = candidates.stream().limit(1).collect(Collectors.toSet());
			alreadyInDatabase.addAll(existing);
			return existing;
		});

		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(50);

		assertEquals(50, codes.size());
		assertTrue(codes.stream().noneMatch(alreadyInDatabase::contains),
				"Ningún código generado debe coincidir con uno ya presente en BD");
	}

	@Test
	void batch_neverReusesReservedCodesFromOngoingLote() {
		when(codigoQrRepository.findExistingCodigosIn(any())).thenReturn(Set.of());
		Set<String> reserved = new HashSet<>(qrCodeGeneratorService.generateUniqueCodes(100));

		List<String> codes = qrCodeGeneratorService.generateUniqueCodes(100, reserved);

		assertEquals(100, codes.size());
		assertTrue(codes.stream().noneMatch(reserved::contains),
				"Ningún código nuevo debe coincidir con los reservados del lote en curso");
	}
}
