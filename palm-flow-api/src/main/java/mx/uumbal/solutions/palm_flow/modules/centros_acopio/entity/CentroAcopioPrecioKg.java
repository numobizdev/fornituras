package mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseUserAuditEntity;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "centro_acopio_precios_kg")
@Getter
@Setter
public class CentroAcopioPrecioKg extends BaseUserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID uuid;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_acopio_uuid", nullable = false)
    private CentroAcopio centroAcopio;

    @Column(name = "precio_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioKg;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDate fechaVigencia;
}
