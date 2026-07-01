package com.numobiz.solutions.fornituras.modules.decommissions.repository;

import com.numobiz.solutions.fornituras.modules.decommissions.entity.DecommissionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecommissionReasonRepository extends JpaRepository<DecommissionReason, Long> {

	List<DecommissionReason> findByActiveTrueOrderByNombre();
}
