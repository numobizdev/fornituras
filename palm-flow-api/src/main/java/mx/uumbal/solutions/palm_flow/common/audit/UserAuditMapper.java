package mx.uumbal.solutions.palm_flow.common.audit;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;

public final class UserAuditMapper {

    private UserAuditMapper() {
    }

    @Value
    @Builder
    public static class Fields {
        Instant createdAt;
        Instant updatedAt;
        Long createdByUserId;
        String createdByUserEmail;
        Long updatedByUserId;
        String updatedByUserEmail;
    }

    public static Fields toFields(BaseUserAuditEntity entity) {
        if (entity == null) {
            return Fields.builder().build();
        }
        return Fields.builder()
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdByUserId(userId(entity.getCreatedByUser()))
                .createdByUserEmail(userEmail(entity.getCreatedByUser()))
                .updatedByUserId(userId(entity.getUpdatedByUser()))
                .updatedByUserEmail(userEmail(entity.getUpdatedByUser()))
                .build();
    }

    private static Long userId(User user) {
        return user != null ? user.getId() : null;
    }

    private static String userEmail(User user) {
        return user != null ? user.getEmail() : null;
    }
}
