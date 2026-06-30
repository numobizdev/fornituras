package com.numobiz.solutions.fornituras.modules.equipment.mapper;

import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentDetail;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;
import org.springframework.stereotype.Component;

/**
 * Convierte {@link Equipment} a sus DTOs. Los nombres de catálogo (tipo/talla/almacén) y la
 * vigencia derivada se reciben ya resueltos por el servicio, que sabe resolverlos en bloque por
 * página para evitar N+1. Mapeo manual (en vez de MapStruct) por la cantidad de campos calculados.
 */
@Component
public class EquipmentMapper {

	public EquipmentSummary toSummary(
			Equipment e, String tipoNombre, String tallaEtiqueta, String almacenNombre, ExpiryStatus vigencia) {
		return new EquipmentSummary(
				e.getId(), e.getCodigoQr(), e.getDescripcion(),
				tipoNombre, tallaEtiqueta, almacenNombre,
				e.getStatus(), vigencia, e.getFechaVencimiento());
	}

	public EquipmentDetail toDetail(
			Equipment e, String tipoNombre, String tallaEtiqueta, String almacenNombre, ExpiryStatus vigencia) {
		return new EquipmentDetail(
				e.getId(), e.getCodigoQr(),
				e.getEquipmentTypeId(), tipoNombre,
				e.getSizeId(), tallaEtiqueta,
				e.getWarehouseId(), almacenNombre,
				e.getStatus(), vigencia,
				e.getDescripcion(), e.getMarca(), e.getModelo(), e.getNivelBalistico(), e.getNumeroInventario(),
				e.getFechaFabricacion(), e.getFechaAdquisicion(), e.getVidaUtilMeses(), e.getFechaVencimiento(),
				e.getObservaciones(), e.getFotoUrl(),
				e.getCreatedAt(), e.getUpdatedAt());
	}
}
