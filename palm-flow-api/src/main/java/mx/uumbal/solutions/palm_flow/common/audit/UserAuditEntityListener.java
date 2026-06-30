package mx.uumbal.solutions.palm_flow.common.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserAuditEntityListener {

    private static AuditUserProvider auditUserProvider;

    @Autowired
    public void setAuditUserProvider(AuditUserProvider provider) {
        UserAuditEntityListener.auditUserProvider = provider;
    }

    @PrePersist
    public void onPrePersist(Object entity) {
        if (!(entity instanceof BaseUserAuditEntity auditable) || auditUserProvider == null) {
            return;
        }
        auditUserProvider.getCurrentUser().ifPresent(user -> {
            auditable.setCreatedByUser(user);
            auditable.setUpdatedByUser(user);
        });
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        if (!(entity instanceof BaseUserAuditEntity auditable) || auditUserProvider == null) {
            return;
        }
        auditUserProvider.getCurrentUser().ifPresent(auditable::setUpdatedByUser);
    }
}
