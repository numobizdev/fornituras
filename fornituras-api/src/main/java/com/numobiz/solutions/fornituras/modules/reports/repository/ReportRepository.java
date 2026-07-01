package com.numobiz.solutions.fornituras.modules.reports.repository;

import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de reporte. La de asignaciones activas hace un <b>join</b> entre {@code Assignment}
 * (vigentes), {@code Equipment} (001) y {@code Officer} (003) para poder filtrar por atributos de
 * fornitura y elemento en el servidor. Los filtros de CURP/RFC usan los <b>blind index</b> (igualdad
 * exacta sin descifrar); el nombre, al estar cifrado de forma no determinista, no se filtra aquí
 * (se resuelve en el servicio). Devuelve tuplas {@code [Assignment, Equipment, Officer]}.
 */
public interface ReportRepository extends Repository<Assignment, Long> {

	@Query("""
			select a, e, o
			from Assignment a
			  join Equipment e on e.id = a.equipmentId
			  join Officer o on o.id = a.officerId
			where a.fechaDevolucion is null
			  and (:qr is null or e.codigoNormalizado like :qr)
			  and (:placa is null or o.placaNormalizada like :placa)
			  and (:curpIdx is null or o.curpIdx = :curpIdx)
			  and (:rfcIdx is null or o.rfcIdx = :rfcIdx)
			  and (:municipio is null or upper(o.municipio) like :municipio)
			order by a.fechaAsignacion desc
			""")
	List<Object[]> findActiveAssignments(
			@Param("qr") String qrLikeNormalized,
			@Param("placa") String placaLikeNormalized,
			@Param("curpIdx") String curpIdx,
			@Param("rfcIdx") String rfcIdx,
			@Param("municipio") String municipioLikeUpper);
}
