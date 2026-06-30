package com.numobiz.solutions.fornituras.modules.qrcodes.entity;

public enum LabelPosition {
	NONE,
	TOP,
	BOTTOM;

	public String getDisplayName() {
		return switch (this) {
			case NONE -> "Solo QR (sin código)";
			case TOP -> "Código arriba del QR";
			case BOTTOM -> "Código abajo del QR";
		};
	}
}
