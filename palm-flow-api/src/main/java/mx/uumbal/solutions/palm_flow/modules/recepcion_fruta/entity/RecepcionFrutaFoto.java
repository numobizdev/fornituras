package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity;

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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseTimestampAuditEntity;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "recepcion_fruta_fotos")
@Getter
@Setter
public class RecepcionFrutaFoto extends BaseTimestampAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID uuid;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recepcion_fruta_uuid", nullable = false)
    private RecepcionFruta recepcionFruta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFotoRecepcion tipo;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
}
