package com.numobiz.solutions.fornituras.modules.transfers.dto;

/** Fornitura incluida en un traslado (referencia + descriptivos, sin PII del elemento). */
public record TransferItemDetail(
		Long equipmentId,
		String codigoQr,
		String descripcion
) {
}
