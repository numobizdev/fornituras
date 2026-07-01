package com.numobiz.solutions.fornituras.modules.transfers.entity;

/**
 * Estado de un traslado entre almacenes. Persiste como cadena (legibilidad + {@code CHECK} en la
 * migración). Ciclo: se crea {@code ENVIADO}; el destino confirma {@code RECIBIDO}; o se
 * {@code CANCELADO} devolviendo las fornituras al origen. Solo {@code ENVIADO} es un traslado en
 * curso (bloquea asignación/baja de sus fornituras).
 */
public enum TransferStatus {
	ENVIADO,
	RECIBIDO,
	CANCELADO
}
