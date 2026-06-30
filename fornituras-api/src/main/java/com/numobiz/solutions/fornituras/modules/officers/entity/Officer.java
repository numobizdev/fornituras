package com.numobiz.solutions.fornituras.modules.officers.entity;

import com.numobiz.solutions.fornituras.common.crypto.EncryptedStringConverter;
import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Elemento policial del padrón. <b>Contiene PII de alta sensibilidad.</b>
 *
 * <p>Las columnas en claro de PII ({@code nombre}, apellidos, {@code curp}, {@code rfc}) se cifran
 * en reposo con {@link EncryptedStringConverter} (AES-GCM, ADR 0006). Como el cifrado es no
 * determinista, la búsqueda por igualdad de {@code curp}/{@code rfc} usa los blind index
 * {@code curpIdx}/{@code rfcIdx} (HMAC). La {@code placa} es identificador operativo: va en claro,
 * única y normalizada. Los catálogos (sexo/tipo de sangre/municipio) se referencian por id.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "officers")
public class Officer extends BaseEntity {

	@Convert(converter = EncryptedStringConverter.class)
	@Column(nullable = false, length = 512)
	private String nombre;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "apellido_paterno", nullable = false, length = 512)
	private String apellidoPaterno;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "apellido_materno", length = 512)
	private String apellidoMaterno;

	/** Identificador operativo (placa/serie); en claro y mostrado al usuario. */
	@Column(nullable = false, length = 40)
	private String placa;

	/** Placa normalizada (trim/upper/sin separadores) que garantiza unicidad. */
	@Column(name = "placa_normalizada", nullable = false, unique = true, length = 40)
	private String placaNormalizada;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 512)
	private String curp;

	/** Blind index HMAC de la CURP (igualdad exacta sin descifrar). */
	@Column(name = "curp_idx", length = 64)
	private String curpIdx;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(length = 512)
	private String rfc;

	@Column(name = "rfc_idx", length = 64)
	private String rfcIdx;

	@Column(name = "sexo_id", nullable = false)
	private Long sexoId;

	@Column(name = "tipo_sangre_id")
	private Long tipoSangreId;

	@Column(name = "municipio_id", nullable = false)
	private Long municipioId;

	/** Referencia a la foto en storage cifrado (gated por ADR 0003); fuera de la fila. */
	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	@Column(nullable = false)
	private boolean active = true;
}
