package mx.uumbal.solutions.palm_flow.modules.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioRepository;
import mx.uumbal.solutions.palm_flow.modules.users.dto.UserRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.users.dto.UserResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.mapper.UserMapper;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CentroAcopioRepository centroAcopioRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        return toResponseWithCentros(user);
    }

    private static final String[] ALLOWED_SORT_FIELDS = {"id", "email", "enabled", "createdAt", "updatedAt"};

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAll(Pageable pageable) {
        Pageable safePageable = toSafePageable(pageable);
        return userRepository.findAll(safePageable).map(this::toResponseWithCentros);
    }

    /**
     * Restricts sort to allowed User entity fields to avoid InvalidDataAccessApiUsageException
     * when client sends invalid sort (e.g. "string" from Swagger).
     */
    private Pageable toSafePageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        }
        java.util.List<Sort.Order> validOrders = sort.stream()
                .filter(order -> isAllowedSortProperty(order.getProperty()))
                .toList();
        Sort safeSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.ASC, "id")
                : Sort.by(validOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

    private boolean isAllowedSortProperty(String property) {
        for (String allowed : ALLOWED_SORT_FIELDS) {
            if (allowed.equals(property)) return true;
        }
        return false;
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        if (userRepository.existsByEmailAndTenantId(dto.getEmail(), tenantId)) {
            throw new BadRequestException("Email already registered");
        }
        User user = userMapper.toEntity(dto);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        Set<Role> roles = resolveRoles(dto.getRoleNames());
        user.setRoles(roles);
        applyCentrosAcopio(user, roles, dto.getCentroAcopioUuids());
        user = userRepository.save(user);
        log.info("User created: {} (tenant: {})", user.getEmail(), tenantId);
        return toResponseWithCentros(user);
    }

    private void applyCentrosAcopio(User user, Set<Role> roles, Set<UUID> centroAcopioUuids) {
        if (hasEncargadoRole(roles)) {
            if (centroAcopioUuids == null || centroAcopioUuids.isEmpty()) {
                throw new BadRequestException("El encargado de centro de acopio debe tener al menos un centro asignado");
            }
            user.setCentrosAcopio(resolveCentrosAcopio(centroAcopioUuids));
        } else {
            user.setCentrosAcopio(new HashSet<>());
        }
    }

    private boolean hasEncargadoRole(Set<Role> roles) {
        return roles.stream().anyMatch(r -> r.getName() == Role.RoleName.ROLE_ENCARGADO_CENTRO_ACOPIO);
    }

    private Set<CentroAcopio> resolveCentrosAcopio(Set<UUID> uuids) {
        List<CentroAcopio> found = centroAcopioRepository.findAllById(uuids);
        if (found.size() != uuids.size()) {
            throw new BadRequestException("Uno o más centros de acopio no existen");
        }
        return new HashSet<>(found);
    }

    private UserResponseDTO toResponseWithCentros(User user) {
        Set<UUID> centroUuids = userRepository.findCentroAcopioUuidsByUserId(user.getId()).stream()
                .collect(Collectors.toSet());
        return userMapper.toResponseDTO(user, centroUuids);
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            Role defaultRole = roleRepository.findByName(Role.RoleName.ROLE_PRODUCTOR)
                    .orElseThrow(() -> new BadRequestException("Default role not found"));
            return Set.of(defaultRole);
        }
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            try {
                Role.RoleName roleName = Role.RoleName.valueOf(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                roleRepository.findByName(roleName).ifPresent(roles::add);
            } catch (IllegalArgumentException ignored) {
                // skip invalid role names
            }
        }
        if (roles.isEmpty()) {
            Role defaultRole = roleRepository.findByName(Role.RoleName.ROLE_PRODUCTOR)
                    .orElseThrow(() -> new BadRequestException("Default role not found"));
            return Set.of(defaultRole);
        }
        return roles;
    }

    public boolean isCurrentUser(Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }
}
