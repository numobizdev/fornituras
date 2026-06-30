package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates a test admin user for development (profile: dev).
 * User: maguilleng@outlook.com / rspoD3v# / ROLE_ADMINISTRADOR / tenant: uumbal
 */
@Slf4j
@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class TestUserDataLoader implements ApplicationRunner {

    private static final String TEST_EMAIL = "maguilleng@outlook.com";
    private static final String TEST_PASSWORD = "rspoD3v#";
    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TenantContext.set(TENANT);
        try {
            if (userRepository.existsByEmailAndTenantId(TEST_EMAIL, TENANT)) {
                log.debug("Test user already exists: {}", TEST_EMAIL);
                return;
            }
            Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMINISTRADOR)
                    .orElseThrow(() -> new IllegalStateException("Role ROLE_ADMINISTRADOR not found. Ensure RoleDataLoader runs first."));
            User user = User.builder()
                    .email(TEST_EMAIL)
                    .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(user);
            log.info("Test admin user created: {} (tenant: {})", TEST_EMAIL, TENANT);
        } finally {
            TenantContext.clear();
        }
    }
}
