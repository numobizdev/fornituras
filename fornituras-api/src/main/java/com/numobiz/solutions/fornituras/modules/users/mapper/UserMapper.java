package com.numobiz.solutions.fornituras.modules.users.mapper;

import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "enabled", ignore = true)
	@Mapping(target = "role", source = "role")
	User toEntity(UserRequestDTO dto);

	UserResponseDTO toResponse(User user);

	List<UserResponseDTO> toResponseList(List<User> users);
}
