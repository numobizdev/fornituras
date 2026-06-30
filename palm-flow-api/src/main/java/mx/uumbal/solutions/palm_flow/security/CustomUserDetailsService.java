package mx.uumbal.solutions.palm_flow.security;

import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Loads user details scoped to the current tenant (TenantContext must be set
 * before this method is called). With @TenantId on the User entity, Hibernate
 * automatically filters findByEmail by the active tenant_id.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        User user = userRepository.findByEmailAndTenantId(username.trim(), tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                true,
                true,
                true,
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                        .collect(Collectors.toList())
        );
    }
}
