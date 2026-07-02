package com.numobiz.solutions.fornituras.modules.media.entity;

/**
 * Contexto de una subida de foto: fija si la imagen es PII y, por tanto, si aplica RBAC + auditoría
 * reforzada y el gating legal de ADR 0003. La foto de {@link #OFFICER} (elemento) es PII; la de
 * {@link #EQUIPMENT} y {@link #EQUIPMENT_TYPE} no lo es.
 */
public enum MediaContext {

	EQUIPMENT(false),
	EQUIPMENT_TYPE(false),
	OFFICER(true);

	private final boolean pii;

	MediaContext(boolean pii) {
		this.pii = pii;
	}

	public boolean isPii() {
		return pii;
	}
}
