package com.numobiz.solutions.fornituras.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BlindIndexerTest {

	private final BlindIndexer indexer = new BlindIndexer("unit-test-blind-index-key");

	@Test
	void sameNormalizedValue_yieldsSameIndex() {
		assertEquals(indexer.index("CURP-800101"), indexer.index("curp 800101"));
	}

	@Test
	void differentValues_yieldDifferentIndexes() {
		assertNotEquals(indexer.index("ABCD800101HDF"), indexer.index("ABCD800101HDG"));
	}

	@Test
	void blankOrNull_isNull() {
		assertNull(indexer.index(null));
		assertNull(indexer.index("   "));
	}
}
