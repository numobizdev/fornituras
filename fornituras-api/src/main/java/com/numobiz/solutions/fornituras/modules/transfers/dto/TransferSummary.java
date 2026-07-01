package com.numobiz.solutions.fornituras.modules.transfers.dto;

import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;

import java.time.LocalDateTime;

/** Fila del listado de traslados: cabecera + conteo de fornituras, sin PII. */
public record TransferSummary(
		Long id,
		Long origenId,
		String origenNombre,
		Long destinoId,
		String destinoNombre,
		TransferStatus status,
		LocalDateTime fechaEnvio,
		LocalDateTime fechaRecepcion,
		long itemCount
) {
}
