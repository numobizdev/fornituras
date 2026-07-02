package com.numobiz.solutions.fornituras.modules.media.service;

/**
 * Puerto de almacenamiento cifrado de objetos binarios (Ports & Adapters, ADR 0016). El dominio
 * ({@link MediaService}) depende de esta abstracción, no del backend concreto: migrar de filesystem
 * local a MinIO/Azure Blob es cambiar de adaptador sin tocar controllers ni servicios (DIP, estilo
 * LEGO). Todo adaptador cifra en reposo (AES-256-GCM a nivel de aplicación); el puerto expone el
 * nonce/IV usado para que los metadatos lo puedan registrar.
 */
public interface FileStoragePort {

	/**
	 * Cifra y persiste {@code content} (imagen saneada, en claro) bajo {@code storageKey}. Devuelve
	 * el nonce/IV empleado y el tamaño del objeto cifrado escrito.
	 */
	StoredObject store(String storageKey, byte[] content);

	/** Carga y descifra el objeto guardado bajo {@code storageKey}. */
	byte[] load(String storageKey);

	/** Elimina el objeto (idempotente: no falla si ya no existe). */
	void delete(String storageKey);

	/** Metadatos del objeto cifrado recién escrito. */
	record StoredObject(byte[] iv, long encryptedSizeBytes) {
	}
}
