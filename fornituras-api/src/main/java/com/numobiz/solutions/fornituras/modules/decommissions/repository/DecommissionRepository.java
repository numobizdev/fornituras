package com.numobiz.solutions.fornituras.modules.decommissions.repository;

import com.numobiz.solutions.fornituras.modules.decommissions.entity.Decommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DecommissionRepository
		extends JpaRepository<Decommission, Long>, JpaSpecificationExecutor<Decommission> {
}
