package com.numobiz.solutions.fornituras.modules.assignments.repository;

import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	boolean existsByEquipmentIdAndFechaDevolucionIsNull(Long equipmentId);

	Optional<Assignment> findByEquipmentIdAndFechaDevolucionIsNull(Long equipmentId);

	Page<Assignment> findByFechaDevolucionIsNull(Pageable pageable);

	Page<Assignment> findByOfficerId(Long officerId, Pageable pageable);
}
