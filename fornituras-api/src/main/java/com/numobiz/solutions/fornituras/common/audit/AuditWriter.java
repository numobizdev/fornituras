package com.numobiz.solutions.fornituras.common.audit;

/**
 * Puerto de auditoría que consume todo el sistema (Principio V). Las features dependen de esta
 * abstracción, no de la tabla ni del mecanismo (LEGO/DIP). La implementación persistente vive en el
 * módulo {@code audit} (feature 012) y escribe en la bitácora append-only, redactando PII.
 *
 * <p>Contrato: nunca registrar PII ni secretos en claro; referenciar entidades por id. El detalle de
 * {@link #recordEvent} debe limitarse a contexto no sensible (tipo de operación, nombres de campos).
 */
public interface AuditWriter {

	/** Registra una acción sobre un recurso identificado por id (p. ej. {@code CREATE_EQUIPMENT}, 42). */
	void record(String action, Long resourceId);

	/**
	 * Registra un evento sin recurso puntual (p. ej. una exportación). El {@code detail} describe el
	 * contexto (tipo de reporte, campos de filtro) y <b>nunca</b> debe contener valores sensibles.
	 */
	void recordEvent(String action, String detail);
}
