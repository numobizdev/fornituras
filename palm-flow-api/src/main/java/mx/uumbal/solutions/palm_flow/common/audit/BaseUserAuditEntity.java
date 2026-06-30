package mx.uumbal.solutions.palm_flow.common.audit;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;

/**
 * Base class for entities that require timestamps and user references for create/update actors.
 */
@MappedSuperclass
@EntityListeners(UserAuditEntityListener.class)
@Getter
@Setter
public abstract class BaseUserAuditEntity extends BaseTimestampAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;
}
