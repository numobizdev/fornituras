package com.numobiz.solutions.fornituras.modules.decommissions.dto;

import java.time.LocalDate;

/**
 * Fila/ficha de una fornitura dada de baja para el listado. Incluye el código, la descripción y el
 * tipo de la fornitura y el motivo resuelto por nombre. Sin PII (el responsable es un id de usuario).
 */
public record DecommissionSummary(
		Long id,
		Long equipmentId,
		String equipmentCodigo,
		String descripcion,
		String tipoNombre,
		Long motivoId,
		String motivoNombre,
		LocalDate fecha,
		Long responsable,
		String observaciones
) {
}
