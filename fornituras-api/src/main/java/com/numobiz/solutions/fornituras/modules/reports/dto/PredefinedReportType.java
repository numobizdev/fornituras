package com.numobiz.solutions.fornituras.modules.reports.dto;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;

/**
 * Reportes operativos predefinidos basados en el estado de la fornitura (001). Los de vigencia
 * (próximos a vencer / caducados) se sirven desde las alertas de 008 y el tablero de 010, y los de
 * movimientos/auditoría dependen de 007/012; por eso no se duplican aquí.
 */
public enum PredefinedReportType {

	INVENTARIO_GENERAL(null),
	DISPONIBLES(EquipmentStatus.DISPONIBLE),
	ASIGNADAS(EquipmentStatus.ASIGNADA),
	MANTENIMIENTO(EquipmentStatus.EN_MANTENIMIENTO),
	BAJA(EquipmentStatus.BAJA_DEFINITIVA);

	private final EquipmentStatus status;

	PredefinedReportType(EquipmentStatus status) {
		this.status = status;
	}

	/** Estado por el que filtra el reporte, o {@code null} para el inventario general (sin filtro). */
	public EquipmentStatus status() {
		return status;
	}
}
