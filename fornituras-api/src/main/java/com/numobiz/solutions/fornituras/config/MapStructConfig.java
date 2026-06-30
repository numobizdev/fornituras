package com.numobiz.solutions.fornituras.config;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.EquipmentTypeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.EquipmentTypeMapperImpl;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.SizeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.SizeMapperImpl;
import com.numobiz.solutions.fornituras.modules.municipios.mapper.MunicipioMapper;
import com.numobiz.solutions.fornituras.modules.municipios.mapper.MunicipioMapperImpl;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapper;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapperImpl;
import com.numobiz.solutions.fornituras.modules.warehouses.mapper.WarehouseMapper;
import com.numobiz.solutions.fornituras.modules.warehouses.mapper.WarehouseMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapStructConfig {

	@Bean
	public UserMapper userMapper() {
		return new UserMapperImpl();
	}

	@Bean
	public EquipmentTypeMapper equipmentTypeMapper() {
		return new EquipmentTypeMapperImpl();
	}

	@Bean
	public SizeMapper sizeMapper() {
		return new SizeMapperImpl();
	}

	@Bean
	public MunicipioMapper municipioMapper() {
		return new MunicipioMapperImpl();
	}

	@Bean
	public WarehouseMapper warehouseMapper() {
		return new WarehouseMapperImpl();
	}
}
