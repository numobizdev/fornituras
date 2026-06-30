package mx.uumbal.solutions.palm_flow.modules.users.mapper;

import mx.uumbal.solutions.palm_flow.modules.users.dto.UserRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.users.dto.UserResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of UserMapper. MapStruct also generates an impl at compile time;
 * this class ensures a bean exists when running from IDE without annotation processing.
 */
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        return User.builder()
                .email(dto.getEmail())
                .enabled(true)
                .build();
    }

    @Override
    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) return null;
        Set<UUID> centroUuids = user.getCentrosAcopio() == null
                ? Set.of()
                : user.getCentrosAcopio().stream()
                        .map(c -> c.getUuid())
                        .collect(Collectors.toSet());
        return toResponseDTO(user, centroUuids);
    }

    @Override
    public UserResponseDTO toResponseDTO(User user, Set<UUID> centroAcopioUuids) {
        if (user == null) return null;
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(rolesToNames(user.getRoles()))
                .centroAcopioUuids(centroAcopioUuids != null ? centroAcopioUuids : Set.of())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
    }
}
