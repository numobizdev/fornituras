package mx.uumbal.solutions.palm_flow.security;

import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.exception.ForbiddenException;
import mx.uumbal.solutions.palm_flow.common.exception.UnauthorizedException;
import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.PredioRepository;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroAcopioScopeService {

    private final UserRepository userRepository;
    private final PredioRepository predioRepository;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        String email = auth.getName();
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public boolean hasFullAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN")
                        || a.equals("ROLE_ADMINISTRADOR")
                        || a.equals("ROLE_ANALISTA"));
    }

    public boolean isEncargado() {
        if (hasFullAccess()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ENCARGADO_CENTRO_ACOPIO"::equals);
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAllowedCentroAcopioUuids() {
        if (hasFullAccess()) {
            return Set.of();
        }
        User user = getCurrentUser();
        return userRepository.findCentroAcopioUuidsByUserId(user.getId()).stream()
                .collect(Collectors.toSet());
    }

    public void requireCentroAcopioAccess(UUID centroAcopioUuid) {
        if (hasFullAccess()) {
            return;
        }
        if (!getAllowedCentroAcopioUuids().contains(centroAcopioUuid)) {
            throw new ForbiddenException("No tiene acceso a este centro de acopio");
        }
    }

    @Transactional(readOnly = true)
    public void requireProductorAccess(UUID productorUuid) {
        if (hasFullAccess()) {
            return;
        }
        Set<UUID> allowed = getAllowedCentroAcopioUuids();
        if (allowed.isEmpty()
                || !predioRepository.existsByProductorUuidAndCentroAcopioUuidIn(productorUuid, allowed)) {
            throw new ForbiddenException("No tiene acceso a este productor");
        }
    }

    public boolean canDeleteProductor(UUID productorUuid) {
        if (hasFullAccess()) {
            return true;
        }
        Set<UUID> allowed = getAllowedCentroAcopioUuids();
        if (allowed.isEmpty()) {
            return false;
        }
        return predioRepository.existsByProductorUuidAndCentroAcopioUuidIn(productorUuid, allowed)
                && !predioRepository.existsByProductorUuidAndCentroAcopioUuidNotIn(productorUuid, allowed);
    }

    public boolean hasEncargadoRole(Set<Role> roles) {
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> r.getName() == Role.RoleName.ROLE_ENCARGADO_CENTRO_ACOPIO);
    }
}
