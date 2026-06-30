package com.numobiz.solutions.fornituras.modules.equipment.service;

import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;

import java.time.LocalDate;

/**
 * Deriva el estado de vigencia de una fornitura a partir de su fecha de vencimiento, sin
 * persistirlo: vencida → {@code CADUCADA}; dentro de la ventana de aviso (≤ 90 días) →
 * {@code PROXIMA_A_VENCER}; en otro caso {@code VIGENTE}. Sin fecha → {@code null}.
 */
public final class ExpiryCalculator {

	/** Ventana de aviso previo al vencimiento (FR-007). */
	public static final int WARNING_WINDOW_DAYS = 90;

	private ExpiryCalculator() {
	}

	public static ExpiryStatus statusFor(LocalDate expiryDate, LocalDate today) {
		if (expiryDate == null) {
			return null;
		}
		if (expiryDate.isBefore(today)) {
			return ExpiryStatus.CADUCADA;
		}
		if (!expiryDate.isAfter(today.plusDays(WARNING_WINDOW_DAYS))) {
			return ExpiryStatus.PROXIMA_A_VENCER;
		}
		return ExpiryStatus.VIGENTE;
	}
}
