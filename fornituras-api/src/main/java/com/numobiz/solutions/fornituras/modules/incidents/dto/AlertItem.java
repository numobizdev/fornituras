package com.numobiz.solutions.fornituras.modules.incidents.dto;

import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;

import java.time.LocalDate;

/**
 * Alerta de vigencia <b>derivada</b> de la fecha de vencimiento de una fornitura (no se materializa).
 * {@code expiryStatus} alimenta el color semántico en la UI: {@code PROXIMA_A_VENCER} = preventiva
 * (naranja), {@code CADUCADA} = crítica (roja). Comparte el umbral (≤ 90 días) con 001/010.
 */
public record AlertItem(
		Long equipmentId,
		String equipmentCodigo,
		String descripcion,
		LocalDate fechaVencimiento,
		ExpiryStatus expiryStatus
) {
}
