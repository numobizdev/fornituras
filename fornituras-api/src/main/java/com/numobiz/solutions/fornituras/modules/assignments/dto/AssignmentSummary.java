package com.numobiz.solutions.fornituras.modules.assignments.dto;

import java.time.LocalDateTime;

/**
 * Fila de la lista de asignaciones (vigentes o historial): qué fornitura tiene quién. Resuelve los
 * datos de fornitura (001) y elemento (003); no expone CURP/RFC.
 */
public record AssignmentSummary(
		Long id,
		Long equipmentId,
		String codigoQr,
		String equipmentDescripcion,
		Long officerId,
		String elementoNombre,
		String placa,
		LocalDateTime fechaAsignacion,
		LocalDateTime fechaDevolucion,
		boolean vigente
) {
}
