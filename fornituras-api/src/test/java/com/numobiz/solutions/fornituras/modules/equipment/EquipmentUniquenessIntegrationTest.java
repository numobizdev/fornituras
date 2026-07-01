package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unicidad del código normalizado, incluida la <b>inserción concurrente</b> del mismo código (T013,
 * SC-002): la restricción {@code UNIQUE(codigo_normalizado)} garantiza cero duplicados aunque dos
 * altas compitan; solo una gana. El perdedor falla —por la comprobación previa o por la restricción
 * de BD—, nunca se crea una segunda fila (ADR 0009: verificado sobre H2; el motor real se validará
 * en CI con Docker).
 */
class EquipmentUniquenessIntegrationTest extends EquipmentApiTestSupport {

	@Autowired
	private EquipmentService equipmentService;

	private EquipmentCreateRequest request(String codigo) {
		return new EquipmentCreateRequest(
				codigo, seed.tipoPrendaId(), seed.tallaId(), seed.warehouseId(),
				"Chaleco", null, null, null, null,
				null, null, null, null, null, null);
	}

	@Test
	void concurrentInsertOfSameCode_onlyOneWins() throws Exception {
		int workers = 2;
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CountDownLatch ready = new CountDownLatch(workers);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger failures = new AtomicInteger();

		for (int i = 0; i < workers; i++) {
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await();
					equipmentService.create(request("FOR-RACE"));
					successes.incrementAndGet();
				} catch (Exception ex) {
					failures.incrementAndGet();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

		assertThat(successes.get()).isEqualTo(1);
		assertThat(failures.get()).isEqualTo(1);
		assertThat(equipmentRepository.existsByCodigoNormalizado("FORRACE")).isTrue();
		assertThat(equipmentRepository.count()).isEqualTo(1);
	}

	@Test
	void sequentialInsertOfNormalizedDuplicate_isRejected() {
		equipmentService.create(request("FOR-777"));

		assertThat(equipmentRepository.count()).isEqualTo(1);
		try {
			equipmentService.create(request("  for - 7 7 7 "));
		} catch (Exception expected) {
			// normalizado idéntico → rechazado
		}
		assertThat(equipmentRepository.count()).isEqualTo(1);
	}
}
