package com.numobiz.solutions.fornituras.modules.municipios.mapper;

import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioSummary;
import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MunicipioMapper {

	MunicipioSummary toSummary(Municipio municipio);
}
