package com.numobiz.solutions.fornituras.modules.transfers.dto;

import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Ficha de un traslado: cabecera + fornituras incluidas. Sin PII. */
public record TransferDetail(
		Long id,
		Long origenId,
		String origenNombre,
		Long destinoId,
		String destinoNombre,
		TransferStatus status,
		LocalDateTime fechaEnvio,
		LocalDateTime fechaRecepcion,
		String observaciones,
		List<TransferItemDetail> items
) {
}
