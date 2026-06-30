package mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseUserAuditEntity;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "centros_acopio")
@Getter
@Setter
public class CentroAcopio extends BaseUserAuditEntity {

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
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CentroAcopioActivo activo;

    @Column(precision = 15, scale = 6)
    private BigDecimal x;

    @Column(precision = 15, scale = 6)
    private BigDecimal y;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitud;

    @Column(length = 200)
    private String encargado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @Column(name = "distancia_km", precision = 10, scale = 3)
    private BigDecimal distanciaKm;

    @Column(length = 100)
    private String alias;

    @Column(length = 300)
    private String direccion;

    @Column(length = 80)
    private String correo;

    @Column(length = 13)
    private String rfc;

    @Column(length = 10)
    private String telefono;
}
