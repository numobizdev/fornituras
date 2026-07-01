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

	/**
	 * Conteo de fornituras agrupado por estado operativo, en una sola consulta agregada. Base del
	 * tablero (010): evita traer registros al cliente y coincide con el {@code COUNT} de cada listado
	 * filtrado por estado.
	 */
	@Query("select e.status as status, count(e) as total from Equipment e group by e.status")
	List<StatusTally> tallyByStatus();

	/**
	 * Fornituras caducadas: vencimiento estrictamente anterior a la fecha dada, excluyendo un estado
	 * (p. ej. baja definitiva). Misma semántica que {@code ExpiryCalculator} (CADUCADA = vencida) para
	 * que el tablero coincida con las alertas de vigencia (008).
	 */
	long countByFechaVencimientoLessThanAndStatusNot(LocalDate today, EquipmentStatus excludedStatus);

	/**
	 * Fornituras próximas a vencer: vencimiento dentro de la ventana de aviso [hoy, hoy+N] (inclusive),
	 * excluyendo un estado. Coincide con la clasificación PROXIMA_A_VENCER de {@code ExpiryCalculator}.
	 */
	long countByFechaVencimientoBetweenAndStatusNot(
			LocalDate from, LocalDate to, EquipmentStatus excludedStatus);

	/** Proyección del conteo por estado (una fila por estado presente en el inventario). */
	interface StatusTally {
		EquipmentStatus getStatus();

		long getTotal();
	}
}
