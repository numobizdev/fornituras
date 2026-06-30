package com.numobiz.solutions.fornituras.modules.assignments;

import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentLifecycleQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentLifecycleQueryTest {

	@Mock
	private AssignmentRepository repository;

	@InjectMocks
	private AssignmentLifecycleQuery query;

	@Test
	void hasActiveAssignment_delegatesToRepository() {
		when(repository.existsByEquipmentIdAndFechaDevolucionIsNull(10L)).thenReturn(true);
		assertTrue(query.hasActiveAssignment(10L));
	}

	@Test
	void noActiveAssignment_isFalse() {
		when(repository.existsByEquipmentIdAndFechaDevolucionIsNull(11L)).thenReturn(false);
		assertFalse(query.hasActiveAssignment(11L));
	}

	@Test
	void ongoingTransfer_isFalseUntilFeature007() {
		assertFalse(query.hasOngoingTransfer(10L));
	}
}
