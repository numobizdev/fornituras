package com.numobiz.solutions.fornituras.modules.equipment.entity;

/**
 * Estado de vigencia <b>derivado</b> de {@code fecha_vencimiento} (no se almacena). Alimenta
 * alertas y color semántico en la UI. {@code null} cuando la fornitura no tiene fecha de
 * vencimiento (consumibles sin caducidad).
 */
public enum ExpiryStatus {
	VIGENTE,
	PROXIMA_A_VENCER,
	CADUCADA
}
