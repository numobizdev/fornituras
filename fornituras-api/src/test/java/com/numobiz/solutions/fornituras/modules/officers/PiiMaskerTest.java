package com.numobiz.solutions.fornituras.modules.officers;

import com.numobiz.solutions.fornituras.modules.officers.service.PiiMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiMaskerTest {

	@Test
	void masksAllButPrefix() {
		String masked = PiiMasker.mask("CURP800101HDFXYZ01");
		assertTrue(masked.startsWith("CURP"));
		assertEquals("CURP", masked.substring(0, 4));
		assertTrue(masked.substring(4).chars().allMatch(c -> c == '•'));
	}

	@Test
	void shortValueFullyMasked() {
		assertEquals("••", PiiMasker.mask("AB"));
	}

	@Test
	void nullPassesThrough() {
		assertNull(PiiMasker.mask(null));
	}
}
