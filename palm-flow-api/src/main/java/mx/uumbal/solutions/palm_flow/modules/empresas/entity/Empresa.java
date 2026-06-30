package mx.uumbal.solutions.palm_flow.modules.empresas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseAuditEntity;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * URL-safe unique identifier used as the tenant discriminator (tenant_id).
     * Once assigned it must not change because it is stored in JWT claims
     * and in every tenant-scoped row across the database.
     */
    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String slug;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
