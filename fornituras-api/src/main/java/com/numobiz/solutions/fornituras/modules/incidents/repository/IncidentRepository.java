package com.numobiz.solutions.fornituras.modules.incidents.repository;

import com.numobiz.solutions.fornituras.modules.incidents.entity.Incident;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

	Page<Incident> findByEstado(IncidentStatus estado, Pageable pageable);
}
