package mx.uumbal.solutions.palm_flow.modules.productores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseUserAuditEntity;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "lotes")
@Getter
@Setter
public class Lote extends BaseUserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID uuid;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_uuid", referencedColumnName = "uuid", nullable = false)
    private Predio predio;

    @Column(name = "anio_plantacion")
    private Integer anioPlantacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EtapaPredio etapa;

    @Column(name = "id_gis", length = 50)
    private String idGis;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitud;

    @Column(precision = 15, scale = 6)
    private BigDecimal x;

    @Column(precision = 15, scale = 6)
    private BigDecimal y;

    @Column(precision = 12, scale = 6)
    private BigDecimal hectareas;

    private boolean ramsar;

    private boolean anp;

    private boolean cambio;

    @Column(nullable = false)
    private Integer eudr = 0;

    @Column(nullable = false)
    private Integer riesgo = 0;

    @Lob
    @Column(columnDefinition = "nvarchar(max)")
    private String wkt;
}
