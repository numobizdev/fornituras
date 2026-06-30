package com.numobiz.solutions.fornituras.modules.equipmenttypes;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.service.NameNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Caracteriza la normalización de nombres usada para la unicidad case/space/acento-insensible.
 */
class NameNormalizerTest {

	@Test
	void collapsesWhitespaceAndCasefolds() {
		assertEquals("chaleco antibala", NameNormalizer.normalize("  Chaleco   Antibala  "));
	}

	@Test
	void stripsAccents() {
		assertEquals("cinturon tactico", NameNormalizer.normalize("Cinturón Táctico"));
	}

	@Test
	void treatsVariantsAsEqual() {
		assertEquals(
				NameNormalizer.normalize("Chaleco Antibalá"),
				NameNormalizer.normalize("chaleco  antibala"));
	}

	@Test
	void nullBecomesEmpty() {
		assertEquals("", NameNormalizer.normalize(null));
	}
}
