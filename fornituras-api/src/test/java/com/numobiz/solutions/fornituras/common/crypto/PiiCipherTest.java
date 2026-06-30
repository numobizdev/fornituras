package com.numobiz.solutions.fornituras.common.crypto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PiiCipherTest {

	@BeforeAll
	static void configureKey() {
		byte[] key = new byte[32];
		for (int i = 0; i < key.length; i++) {
			key[i] = (byte) i;
		}
		PiiCipher.configure(key);
	}

	@Test
	void encryptThenDecrypt_roundTrips() {
		String plain = "Juan Pérez Curp123";
		String cipher = PiiCipher.encrypt(plain);
		assertNotEquals(plain, cipher);
		assertEquals(plain, PiiCipher.decrypt(cipher));
	}

	@Test
	void encryptIsNonDeterministic() {
		String plain = "CURP800101HDFXYZ01";
		assertNotEquals(PiiCipher.encrypt(plain), PiiCipher.encrypt(plain));
	}

	@Test
	void nullPassesThrough() {
		assertNull(PiiCipher.encrypt(null));
		assertNull(PiiCipher.decrypt(null));
	}
}
