package mx.uumbal.solutions.palm_flow.common.audit;

import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditUserProvider {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        return userRepository.findByEmailAndTenantId(auth.getName(), tenantId);
    }
}
