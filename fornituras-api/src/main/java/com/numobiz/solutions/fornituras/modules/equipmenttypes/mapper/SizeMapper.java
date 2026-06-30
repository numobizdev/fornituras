package com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.Size;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SizeMapper {

	@Mapping(target = "equipmentTypeId", source = "equipmentType.id")
	SizeSummary toSummary(Size size);

	List<SizeSummary> toSummaryList(List<Size> sizes);
}
