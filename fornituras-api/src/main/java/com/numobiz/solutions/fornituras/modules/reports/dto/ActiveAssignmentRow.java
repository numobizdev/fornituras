package com.numobiz.solutions.fornituras.modules.reports.dto;

import java.time.LocalDateTime;

/**
 * Fila del reporte de asignaciones activas: qué fornitura tiene qué elemento, con datos operativos.
 * La CURP/RFC llegan <b>enmascaradas salvo para roles autorizados</b> (mismo criterio que 003);
 * {@code piiMasked} indica si esos campos vienen ocultos. Se usa tanto en pantalla como en el Excel.
 */
public record ActiveAssignmentRow(
		Long assignmentId,
		Long equipmentId,
		String codigoQr,
		String equipmentDescripcion,
		Long officerId,
		String elementoNombre,
		String placa,
		String curp,
		String rfc,
		String municipio,
		String estado,
		boolean piiMasked,
		LocalDateTime fechaAsignacion) {
}
