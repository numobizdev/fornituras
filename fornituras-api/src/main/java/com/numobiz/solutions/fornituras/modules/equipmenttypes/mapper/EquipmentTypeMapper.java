package com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeDetail;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EquipmentTypeMapper {

	EquipmentTypeSummary toSummary(EquipmentType type);

	List<EquipmentTypeSummary> toSummaryList(List<EquipmentType> types);

	@Mapping(target = "sizes", source = "sizes")
	EquipmentTypeDetail toDetail(EquipmentType type, List<SizeSummary> sizes);
}
