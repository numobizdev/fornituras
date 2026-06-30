package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioRepository;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates a test encargado de centro de acopio user for development (profile: dev).
 * User: maguillengo@gmail.com / C3ntr0Ac0p10 / ROLE_ENCARGADO_CENTRO_ACOPIO / tenant: uumbal
 */
@Slf4j
@Component
@Profile("dev")
@Order(7)
@RequiredArgsConstructor
public class EncargadoCentroAcopioUserDataLoader implements ApplicationRunner {

    private static final String TEST_EMAIL = "maguillengo@gmail.com";
    private static final String TEST_PASSWORD = "C3ntr0Ac0p10";
    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CentroAcopioRepository centroAcopioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TenantContext.set(TENANT);
        try {
            Role encargadoRole = roleRepository.findByName(Role.RoleName.ROLE_ENCARGADO_CENTRO_ACOPIO)
                    .orElseThrow(() -> new IllegalStateException(
                            "Role ROLE_ENCARGADO_CENTRO_ACOPIO not found. Ensure RoleDataLoader runs first."));

            User user = userRepository.findByEmailAndTenantId(TEST_EMAIL, TENANT).orElse(null);
            if (user == null) {
                user = User.builder()
                        .email(TEST_EMAIL)
                        .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                        .enabled(true)
                        .roles(Set.of(encargadoRole))
                        .build();
                user = userRepository.save(user);
                log.info("Encargado centro acopio test user created: {} (tenant: {})", TEST_EMAIL, TENANT);
            } else {
                log.debug("Encargado centro acopio test user already exists: {}", TEST_EMAIL);
            }

            if (user.getCentrosAcopio() == null || user.getCentrosAcopio().isEmpty()) {
                assignSampleCentros(user);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void assignSampleCentros(User user) {
        List<CentroAcopio> sampleCentros = centroAcopioRepository.findAll(PageRequest.of(0, 2)).getContent();
        if (sampleCentros.isEmpty()) {
            log.warn("No centros de acopio available to assign to encargado test user");
            return;
        }

        Set<CentroAcopio> assigned = new HashSet<>(sampleCentros);
        user.setCentrosAcopio(assigned);
        userRepository.save(user);
        log.info("Assigned {} centro(s) de acopio to encargado test user {}", assigned.size(), TEST_EMAIL);
    }
}
