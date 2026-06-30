package com.numobiz.solutions.fornituras.modules.officers.repository;

import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OfficerRepository
		extends JpaRepository<Officer, Long>, JpaSpecificationExecutor<Officer> {

	boolean existsByPlacaNormalizada(String placaNormalizada);

	boolean existsByCurpIdx(String curpIdx);

	boolean existsByRfcIdx(String rfcIdx);
}
