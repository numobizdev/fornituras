package com.numobiz.solutions.fornituras.config;

import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapper;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapStructConfig {

	@Bean
	public UserMapper userMapper() {
		return new UserMapperImpl();
	}
}
