package com.numobiz.solutions.fornituras.modules.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadatos de una imagen almacenada de forma segura (017, ADR 0016). La imagen <b>no</b> vive en esta
 * fila: se guarda cifrada (AES-256-GCM) en el filesystem bajo {@link #storageKey}. Esta entidad
 * <b>no contiene PII</b>; {@link #pii} marca la sensibilidad para gobernar RBAC + auditoría del acceso.
 *
 * <p>El {@code id} es un UUID opaco (no enumerable), lo que las entidades de dominio referencian desde
 * su {@code foto_url}. No extiende {@code BaseEntity} porque su clave es UUID (no la secuencia Long del
 * resto del inventario).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media_asset")
public class MediaAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Tipo final tras re-codificar y eliminar EXIF ({@code image/jpeg|png|webp}). */
	@Column(name = "content_type", nullable = false, length = 50)
	private String contentType;

	/** Tamaño de la imagen saneada (en claro), en bytes. */
	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	/** Huella SHA-256 de la imagen saneada (deduplicación/integridad). */
	@Column(nullable = false, length = 64)
	private String sha256;

	/** Referencia opaca al objeto cifrado en storage (ruta relativa dentro del dir de media). */
	@Column(name = "storage_key", nullable = false, unique = true, length = 255)
	private String storageKey;

	/** Nonce/IV del cifrado AES-256-GCM del objeto (también embebido al frente del objeto en disco). */
	@Column(name = "iv", length = 16)
	private byte[] iv;

	/** {@code true} si es foto de elemento (PII): activa RBAC + auditoría reforzada. */
	@Column(name = "is_pii", nullable = false)
	private boolean pii;

	/** Id lógico del usuario que subió la foto (para auditoría); sin FK física (desacople). */
	@Column(name = "uploaded_by")
	private Long uploadedBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
