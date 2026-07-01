package com.numobiz.solutions.fornituras.modules.catalog.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cabecera de un catálogo administrable (p. ej. {@code TIPO_FORNITURA}, {@code TALLA},
 * {@code TIPO_ALMACEN}). Sustituye a las antiguas tablas tipadas por catálogo (ADR 0007): un solo
 * par {@code catalog}/{@code catalog_item} sirve a todos los catálogos planos del sistema.
 *
 * <p>El {@code code} es la clave estable e inmutable por la que los consumidores localizan el
 * catálogo. Un catálogo de sistema ({@code system = true}) es semilla y no puede borrarse; solo se
 * administran sus valores.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "catalog")
public class Catalog extends BaseEntity {

	@Column(nullable = false, unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(length = 500)
	private String descripcion;

	@Column(name = "is_system", nullable = false)
	private boolean system = false;

	@Column(nullable = false)
	private boolean active = true;
}
