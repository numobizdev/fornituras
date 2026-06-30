package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseTimestampAuditEntity;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "recepciones_fruta")
@Getter
@Setter
public class RecepcionFruta extends BaseTimestampAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID uuid;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @Column(nullable = false)
    private Instant fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_acopio_uuid", nullable = false)
    private CentroAcopio centroAcopio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productor_uuid", referencedColumnName = "uuid", nullable = false)
    private Productor productor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_uuid", referencedColumnName = "uuid", nullable = false)
    private Predio predio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_uuid", referencedColumnName = "uuid")
    private Lote lote;

    @Column(length = 20)
    private String placa;

    @Column(length = 100)
    private String modelo;

    @Column(length = 100)
    private String marca;

    @Column(name = "tipo_color", length = 50)
    private String tipoColor;

    @Column(length = 200)
    private String propietario;

    @Column(name = "peso_bruto", precision = 12, scale = 3)
    private BigDecimal pesoBruto;

    @Column(name = "peso_tara", precision = 12, scale = 3)
    private BigDecimal pesoTara;

    @Column(name = "peso_neto", precision = 12, scale = 3)
    private BigDecimal pesoNeto;

    @Column(name = "precio_kg", precision = 10, scale = 2)
    private BigDecimal precioKg;

    @Column(name = "monto_a_pagar", precision = 14, scale = 2)
    private BigDecimal montoAPagar;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_peso", length = 20)
    private OrigenPeso origenPeso;

    @Convert(converter = CalidadFrutaListConverter.class)
    @Column(name = "calidad_fruta", length = 255)
    private List<CalidadFruta> calidadFruta = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "registro_offline", nullable = false)
    private boolean registroOffline = false;

    @Column(name = "fecha_sync")
    private Instant fechaSync;
}
