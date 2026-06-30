package com.numobiz.solutions.fornituras.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeNormalizerTest {

	@Test
	void removesHyphensAndUppercases() {
		assertEquals("FOR001", CodeNormalizer.normalize("for-001"));
	}

	@Test
	void collapsesSpacesAndHyphens() {
		assertEquals("FOR001", CodeNormalizer.normalize("  for - 0 0 1 "));
	}

	@Test
	void nullBecomesEmpty() {
		assertEquals("", CodeNormalizer.normalize(null));
	}
}
