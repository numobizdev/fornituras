package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFrutaFoto;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.TipoFotoRecepcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecepcionFrutaFotoRepository extends JpaRepository<RecepcionFrutaFoto, UUID> {

    List<RecepcionFrutaFoto> findByRecepcionFrutaUuid(UUID recepcionFrutaUuid);

    Optional<RecepcionFrutaFoto> findByRecepcionFrutaUuidAndTipo(UUID recepcionFrutaUuid, TipoFotoRecepcion tipo);
}
