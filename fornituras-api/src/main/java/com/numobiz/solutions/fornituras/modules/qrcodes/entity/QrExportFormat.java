package com.numobiz.solutions.fornituras.modules.qrcodes.entity;

public enum QrExportFormat {
	PDF("PDF para impresión"),
	ZIP("ZIP con imágenes PNG");

	private final String displayName;

	QrExportFormat(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isPdf() {
		return this == PDF;
	}
}
