package com.numobiz.solutions.fornituras.modules.audit.repository;

import com.numobiz.solutions.fornituras.modules.audit.entity.AuditLog;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la bitácora. <b>Append-only por diseño</b> (FR-003): extiende solo el marcador
 * {@code Repository}, por lo que expone únicamente {@link #save} (inserción) y la consulta filtrada;
 * <b>no</b> hereda operaciones de update/delete (ni de {@code CrudRepository} ni el
 * {@code delete(Specification)} de {@code JpaSpecificationExecutor}). La inmutabilidad se refuerza en
 * BD con triggers (ADR 0012).
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

	AuditLog save(AuditLog auditLog);

	/**
	 * Consulta paginada y filtrable. Los parámetros ya vienen normalizados por el servicio: {@code actor}
	 * como patrón {@code %...%} en minúsculas; {@code accion}/{@code entidad} en mayúsculas exactas.
	 * Nulo = sin filtrar por ese campo.
	 */
	@Query("""
			select a from AuditLog a
			where (:actor is null or lower(a.actor) like :actor)
			  and (:accion is null or upper(a.accion) = :accion)
			  and (:entidad is null or upper(a.entidad) = :entidad)
			  and (:desde is null or a.occurredAt >= :desde)
			  and (:hasta is null or a.occurredAt <= :hasta)
			""")
	Page<AuditLog> search(
			@Param("actor") String actorLikeLower,
			@Param("accion") String accionUpper,
			@Param("entidad") String entidadUpper,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta,
			Pageable pageable);
}
