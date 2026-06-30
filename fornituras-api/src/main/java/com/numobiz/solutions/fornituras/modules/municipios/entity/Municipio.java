package com.numobiz.solutions.fornituras.modules.municipios.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo geográfico de municipios. Dato de referencia compartido por almacenes (005) y, a
 * futuro, por elementos (003). Sin PII.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "municipio")
public class Municipio extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String nombre;

	/**
	 * Nombre normalizado (trim + colapso de espacios + casefold) para unicidad
	 * case/space/acento-insensible. El usuario nunca lo ve; solo se compara.
	 */
	@Column(name = "nombre_normalizado", nullable = false, unique = true, length = 120)
	private String nombreNormalizado;

	@Column(nullable = false)
	private boolean active = true;
}
