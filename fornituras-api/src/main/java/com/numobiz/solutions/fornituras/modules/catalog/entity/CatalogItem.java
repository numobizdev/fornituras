package com.numobiz.solutions.fornituras.modules.catalog.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valor de un catálogo. Pertenece a un {@link Catalog} y opcionalmente cuelga de otro valor
 * ({@code parentItem}) para modelar catálogos dependientes sin tablas extra: p. ej. una talla
 * ({@code TALLA}) ligada a un tipo de prenda ({@code TIPO_PRENDA}). Sin padre = valor global.
 *
 * <p>El {@code nombreNormalizado} (trim + colapso de espacios + casefold + sin acentos) garantiza
 * unicidad case/space/acento-insensible dentro del catálogo. Los consumidores (equipo, almacén)
 * referencian el valor por su id.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "catalog_item")
public class CatalogItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "catalog_id", nullable = false)
	private Catalog catalog;

	/** Clave opcional estable dentro del catálogo (p. ej. CENTRAL/REGIONAL para TIPO_ALMACEN). */
	@Column(length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(name = "nombre_normalizado", nullable = false, length = 120)
	private String nombreNormalizado;

	@Column(length = 500)
	private String descripcion;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	/** Enlace opcional a un valor padre (jerarquía item→item); NULL = valor global. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_item_id")
	private CatalogItem parentItem;

	private Integer orden;

	@Column(nullable = false)
	private boolean active = true;
}
