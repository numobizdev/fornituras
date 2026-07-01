package com.numobiz.solutions.fornituras.modules.incidents.repository;

import com.numobiz.solutions.fornituras.modules.incidents.entity.Incident;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

	Page<Incident> findByEstado(IncidentStatus estado, Pageable pageable);

	/**
	 * Número de fornituras <b>distintas</b> con al menos una incidencia en alguno de los estados dados
	 * (p. ej. abiertas/en proceso). Base del contador "con incidencia" del reporte (011), como agregado
	 * sin traer registros.
	 */
	@Query("select count(distinct i.equipmentId) from Incident i where i.estado in :estados")
	long countDistinctEquipmentByEstadoIn(@Param("estados") Collection<IncidentStatus> estados);
}
