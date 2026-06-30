package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;
import com.numobiz.solutions.fornituras.modules.equipment.service.ExpiryCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpiryCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

	@Test
	void noExpiryDate_isNull() {
		assertNull(ExpiryCalculator.statusFor(null, TODAY));
	}

	@Test
	void pastDate_isExpired() {
		assertEquals(ExpiryStatus.CADUCADA, ExpiryCalculator.statusFor(TODAY.minusDays(1), TODAY));
	}

	@Test
	void withinWarningWindow_isNearExpiry() {
		assertEquals(ExpiryStatus.PROXIMA_A_VENCER, ExpiryCalculator.statusFor(TODAY.plusDays(30), TODAY));
	}

	@Test
	void exactlyNinetyDays_isNearExpiry() {
		assertEquals(ExpiryStatus.PROXIMA_A_VENCER, ExpiryCalculator.statusFor(TODAY.plusDays(90), TODAY));
	}

	@Test
	void beyondWarningWindow_isValid() {
		assertEquals(ExpiryStatus.VIGENTE, ExpiryCalculator.statusFor(TODAY.plusDays(91), TODAY));
	}
}
