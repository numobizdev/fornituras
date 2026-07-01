package com.numobiz.solutions.fornituras.modules.audit;

import com.numobiz.solutions.fornituras.modules.audit.repository.AuditLogRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inmutabilidad a nivel de aplicación (T014/SC-003): el repositorio de auditoría no expone
 * operaciones de borrado ni actualización (no hereda de {@code CrudRepository}). En BD, además, los
 * triggers de la migración rechazan UPDATE/DELETE (ADR 0012).
 */
class AuditImmutabilityTest {

	@Test
	void repository_doesNotExposeDeleteOrCrud() {
		assertThat(CrudRepository.class.isAssignableFrom(AuditLogRepository.class)).isFalse();

		boolean hasMutation = Arrays.stream(AuditLogRepository.class.getMethods())
				.map(Method::getName)
				.anyMatch(name -> name.startsWith("delete") || name.startsWith("removeById"));

		assertThat(hasMutation).isFalse();
	}
}
