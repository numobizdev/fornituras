package mx.uumbal.solutions.palm_flow.modules.users.repository;

import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Native {@code SELECT *} with explicit {@code tenant_id} so login does not depend on Hibernate
     * applying the {@code @TenantId} predicate to JPQL (which was returning no rows even when the
     * user existed). Roles load via the entity's eager mapping after the row is managed.
     */
    @Query(
            value =
                    "SELECT * FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) "
                            + "AND TRIM(tenant_id) = TRIM(:tenantId)",
            nativeQuery = true)
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") String tenantId);

    /**
     * Raw SQL count so we see rows regardless of Hibernate {@code @TenantId} filter interaction
     * with derived {@code existsBy...} queries (which can miss existing users and cause duplicate
     * inserts on {@code uk_users_email_tenant}).
     */
    @Query(
            value =
                    "SELECT COUNT(*) FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) "
                            + "AND TRIM(tenant_id) = TRIM(:tenantId)",
            nativeQuery = true)
    long countByEmailAndTenantIdRaw(@Param("email") String email, @Param("tenantId") String tenantId);

    default boolean existsByEmailAndTenantId(String email, String tenantId) {
        return countByEmailAndTenantIdRaw(email, tenantId) > 0;
    }

    Page<User> findAll(Pageable pageable);

    /**
     * Native SQL migration: assigns a tenant_id to all rows that currently
     * have no tenant, bypassing Hibernate's @TenantId session filter.
     * Executed once by SuperAdminDataLoader on first startup.
     */
    @Modifying
    @Query(value = "UPDATE users SET tenant_id = :tenantId WHERE tenant_id IS NULL", nativeQuery = true)
    int migrateOrphanedUsersToTenant(@Param("tenantId") String tenantId);

    @Query(
            value = "SELECT centro_acopio_uuid FROM user_centros_acopio WHERE user_id = :userId",
            nativeQuery = true)
    List<UUID> findCentroAcopioUuidsByUserId(@Param("userId") Long userId);
}
