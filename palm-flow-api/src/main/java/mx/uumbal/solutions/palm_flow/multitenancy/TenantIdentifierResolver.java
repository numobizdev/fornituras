package mx.uumbal.solutions.palm_flow.multitenancy;

import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier for Hibernate's @TenantId discriminator.
 * Reads from TenantContext (ThreadLocal). When unset or blank, uses
 * {@link EmpresaDataLoader#UUMBAL_SLUG} so tenant-scoped persistence matches auth defaults.
 * Data loaders that operate on tenant-scoped entities must call
 * TenantContext.set(...) before any JPA operations when a non-default tenant is required.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return EmpresaDataLoader.slugOrDefault(TenantContext.get());
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
