package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.empresas.entity.Empresa;
import mx.uumbal.solutions.palm_flow.modules.empresas.repository.EmpresaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the initial UUMBAL empresa on first startup (all profiles).
 * Runs after RoleDataLoader (Order 1) and before SuperAdminDataLoader (Order 3).
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class EmpresaDataLoader implements ApplicationRunner {

    public static final String UUMBAL_SLUG = "uumbal";

    /**
     * Returns {@link #UUMBAL_SLUG} when the slug is null, empty, or whitespace-only.
     */
    public static String slugOrDefault(String slug) {
        if (slug == null || slug.isBlank()) {
            return UUMBAL_SLUG;
        }
        return slug.trim();
    }

    private final EmpresaRepository empresaRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (empresaRepository.existsBySlug(UUMBAL_SLUG)) {
            log.debug("Empresa UUMBAL already exists, skipping seed");
            return;
        }
        Empresa uumbal = Empresa.builder()
                .nombre("UUMBAL")
                .slug(UUMBAL_SLUG)
                .activo(true)
                .build();
        empresaRepository.save(uumbal);
        log.info("Empresa UUMBAL created (slug: {})", UUMBAL_SLUG);
    }
}
