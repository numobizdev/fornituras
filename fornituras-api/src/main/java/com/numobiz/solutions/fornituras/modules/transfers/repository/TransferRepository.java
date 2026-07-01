package com.numobiz.solutions.fornituras.modules.transfers.repository;

import com.numobiz.solutions.fornituras.modules.transfers.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a traslados. {@link JpaSpecificationExecutor} habilita el filtrado dinámico del listado
 * (origen/destino/estado) sin multiplicar métodos derivados.
 */
public interface TransferRepository
		extends JpaRepository<Transfer, Long>, JpaSpecificationExecutor<Transfer> {

	/**
	 * ¿La fornitura está en un traslado en curso (estado ENVIADO)? Alimenta el puerto de ciclo de
	 * vida para bloquear su asignación/baja mientras viaja.
	 */
	@Query("""
			select case when count(i) > 0 then true else false end
			from TransferItem i
			join Transfer t on t.id = i.transferId
			where i.equipmentId = :equipmentId and t.status = 'ENVIADO'
			""")
	boolean existsOngoingByEquipmentId(@Param("equipmentId") Long equipmentId);
}
