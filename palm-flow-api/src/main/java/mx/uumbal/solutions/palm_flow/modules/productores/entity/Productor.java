package mx.uumbal.solutions.palm_flow.modules.productores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mx.uumbal.solutions.palm_flow.common.audit.BaseUserAuditEntity;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "productores")
@Getter
@Setter
public class Productor extends BaseUserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID uuid;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "nombre_2", length = 200)
    private String nombre2;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Genero genero;

    @Column(length = 20)
    private String telefono;

    @Column(name = "correo_electronico", length = 150)
    private String correoElectronico;

    @Column(length = 13)
    private String rfc;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", length = 10)
    private TipoPersona tipoPersona;

    @Column(nullable = false)
    private boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Frecuente frecuente;

    @Column(name = "type_cert", length = 50)
    private String typeCert;

    @Column(name = "id_akk", length = 50)
    private String idAkk;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_rspo", length = 30)
    private NivelRspo nivelRspo;
}
