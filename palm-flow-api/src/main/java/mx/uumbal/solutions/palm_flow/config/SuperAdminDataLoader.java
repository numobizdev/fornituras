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
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Runs on every startup (all profiles). Responsibilities:
 * 1. Migrates existing users with NULL tenant_id to UUMBAL using native SQL
 *    (bypasses Hibernate @TenantId session filter intentionally).
 * 2. Creates the platform super admin user (contacto@numobiz.net) if not present.
 *
 * Must run after EmpresaDataLoader (Order 2) so that the UUMBAL empresa exists.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class SuperAdminDataLoader implements ApplicationRunner {

    private static final String SUPER_ADMIN_EMAIL = "contacto@numobiz.net";
    private static final String SUPER_ADMIN_PASSWORD = "rspoD3v#";
    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateOrphanedUsers();
        createSuperAdminIfAbsent();
    }

    private void migrateOrphanedUsers() {
        int migrated = userRepository.migrateOrphanedUsersToTenant(TENANT);
        if (migrated > 0) {
            log.info("Migrated {} existing user(s) to tenant '{}'", migrated, TENANT);
        }
    }

    private void createSuperAdminIfAbsent() {
        TenantContext.set(TENANT);
        try {
            if (userRepository.existsByEmailAndTenantId(SUPER_ADMIN_EMAIL, TENANT)) {
                log.debug("Super admin already exists: {}", SUPER_ADMIN_EMAIL);
                return;
            }
            Role superAdminRole = roleRepository.findByName(Role.RoleName.ROLE_SUPER_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found. Ensure RoleDataLoader runs first."));
            User superAdmin = User.builder()
                    .email(SUPER_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(SUPER_ADMIN_PASSWORD))
                    .enabled(true)
                    .roles(Set.of(superAdminRole))
                    .build();
            userRepository.save(superAdmin);
            log.info("Super admin created: {} (tenant: {})", SUPER_ADMIN_EMAIL, TENANT);
        } finally {
            TenantContext.clear();
        }
    }
}
