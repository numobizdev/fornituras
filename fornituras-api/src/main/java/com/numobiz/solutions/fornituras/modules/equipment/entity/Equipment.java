package com.numobiz.solutions.fornituras.modules.equipment.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Fornitura: el equipo físico controlado por el inventario (chaleco, cinturón, casco, etc.).
 *
 * <p>El {@code id} (heredado de {@link BaseEntity}) es el identificador interno opaco e inmutable;
 * el {@code codigoQr} es el identificador físico grabado/impreso y único. Las referencias a
 * tipo/talla/almacén se guardan como id (FK en BD) para mantener el módulo desacoplado de
 * {@code equipmenttypes} y {@code warehouses}; la integridad la garantiza la migración. La
 * fornitura <b>no</b> guarda PII del elemento: la asignación vive en la feature 004.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

	/** Código físico (QR/serie) tal cual se muestra; recortado y en mayúsculas. */
	@Column(name = "codigo_qr", nullable = false, length = 60)
	private String codigoQr;

	/** Código normalizado (sin espacios/guiones, mayúsculas) que garantiza unicidad física. */
	@Column(name = "codigo_normalizado", nullable = false, unique = true, length = 60)
	private String codigoNormalizado;

	@Column(name = "equipment_type_id", nullable = false)
	private Long equipmentTypeId;

	@Column(name = "size_id")
	private Long sizeId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EquipmentStatus status = EquipmentStatus.DISPONIBLE;

	@Column(length = 255)
	private String descripcion;

	@Column(length = 120)
	private String marca;

	@Column(length = 120)
	private String modelo;

	@Column(name = "nivel_balistico", length = 60)
	private String nivelBalistico;

	@Column(name = "numero_inventario", length = 60)
	private String numeroInventario;

	@Column(name = "fecha_fabricacion")
	private LocalDate fechaFabricacion;

	@Column(name = "fecha_adquisicion")
	private LocalDate fechaAdquisicion;

	@Column(name = "vida_util_meses")
	private Integer vidaUtilMeses;

	/** Dato canónico de vida útil: fecha absoluta de la que se derivan vigencia y alertas. */
	@Column(name = "fecha_vencimiento")
	private LocalDate fechaVencimiento;

	@Column(length = 500)
	private String observaciones;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;
}
