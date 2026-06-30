package com.numobiz.solutions.fornituras.modules.warehouses.mapper;

import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseDetail;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseSummary;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

	WarehouseSummary toSummary(Warehouse warehouse);

	@Mapping(target = "ocupacion", source = "ocupacion")
	@Mapping(target = "porcentajeOcupacion", source = "porcentajeOcupacion")
	WarehouseDetail toDetail(Warehouse warehouse, long ocupacion, Double porcentajeOcupacion);
}
