package mx.uumbal.solutions.palm_flow.modules.users.mapper;

import mx.uumbal.solutions.palm_flow.modules.users.dto.UserRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.users.dto.UserResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;

import java.util.Set;
import java.util.UUID;

/**
 * Maps between User entity and DTOs. Implementation is UserMapperImpl (explicit bean for IDE runs).
 */
public interface UserMapper {

    User toEntity(UserRequestDTO dto);

    UserResponseDTO toResponseDTO(User user);

    UserResponseDTO toResponseDTO(User user, Set<UUID> centroAcopioUuids);

    Set<String> rolesToNames(Set<Role> roles);
}
