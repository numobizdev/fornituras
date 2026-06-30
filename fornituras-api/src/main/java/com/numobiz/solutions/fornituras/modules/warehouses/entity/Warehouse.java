package com.numobiz.solutions.fornituras.modules.warehouses.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Almacén: entidad operativa (no catálogo plano). Ubicación física de resguardo de fornituras,
 * con clave de negocio, clasificación, ubicación, responsable y cupo.
 *
 * <p>Las referencias a municipio y responsable se guardan como id (FK en BD) en lugar de
 * asociaciones JPA para mantener el módulo desacoplado de {@code municipios} y {@code users};
 * la integridad referencial la garantiza la migración. Sin PII de elementos; los campos
 * sensibles (dirección, geolocalización, responsable, contacto) se protegen por autorización.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouse")
public class Warehouse extends BaseEntity {

	/** Clave de negocio estable (p. ej. "ALM-01"); referenciada por traslados y etiquetas. */
	@Column(nullable = false, unique = true, length = 40)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String nombre;

	/** Nombre normalizado (trim + colapso + casefold + sin acentos) para unicidad. */
	@Column(name = "nombre_normalizado", nullable = false, unique = true, length = 120)
	private String nombreNormalizado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WarehouseType tipo;

	@Column(name = "municipio_id")
	private Long municipioId;

	@Column(length = 255)
	private String direccion;

	@Column(length = 10)
	private String cp;

	@Column(precision = 9, scale = 6)
	private BigDecimal latitud;

	@Column(precision = 9, scale = 6)
	private BigDecimal longitud;

	@Column(name = "responsable_id")
	private Long responsableId;

	@Column(length = 30)
	private String telefono;

	@Column(name = "email_contacto", length = 255)
	private String emailContacto;

	private Integer capacidad;

	@Column(length = 500)
	private String observaciones;

	@Column(nullable = false)
	private boolean active = true;
}
