package com.numobiz.solutions.fornituras.modules.equipment.repository;

import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a fornituras. {@link JpaSpecificationExecutor} habilita el filtrado dinámico del listado
 * (estado/tipo/talla/almacén/código/descripción) sin multiplicar métodos derivados.
 */
public interface EquipmentRepository
		extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

	boolean existsByCodigoNormalizado(String codigoNormalizado);

	Optional<Equipment> findByCodigoNormalizado(String codigoNormalizado);

	/**
	 * Fornituras con vencimiento hasta la fecha dada (próximas a vencer o ya caducadas), excluyendo un
	 * estado (p. ej. baja definitiva). Base de las alertas de vigencia derivadas (008): solo devuelve
	 * las candidatas dentro de la ventana de aviso, para clasificarlas con {@code ExpiryCalculator}.
	 */
	List<Equipment> findByFechaVencimientoLessThanEqualAndStatusNot(LocalDate threshold, EquipmentStatus excludedStatus);

	/** Ids de las fornituras de un tipo dado; sirve para filtrar por tipo sin acoplar otros módulos a la entidad. */
	@Query("select e.id from Equipment e where e.equipmentTypeId = :typeId")
	List<Long> findIdsByEquipmentTypeId(@Param("typeId") Long typeId);
}
