package com.numobiz.solutions.fornituras.modules.equipment.repository;

import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Acceso a fornituras. {@link JpaSpecificationExecutor} habilita el filtrado dinámico del listado
 * (estado/tipo/talla/almacén/código/descripción) sin multiplicar métodos derivados.
 */
public interface EquipmentRepository
		extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

	boolean existsByCodigoNormalizado(String codigoNormalizado);

	Optional<Equipment> findByCodigoNormalizado(String codigoNormalizado);
}
