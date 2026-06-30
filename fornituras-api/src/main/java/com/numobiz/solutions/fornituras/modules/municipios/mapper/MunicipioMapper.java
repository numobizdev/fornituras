package com.numobiz.solutions.fornituras.modules.municipios.mapper;

import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioSummary;
import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import org.mapstruct.Mapper;

@Mapper
public interface MunicipioMapper {

	MunicipioSummary toSummary(Municipio municipio);
}
