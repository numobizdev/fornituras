package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentLifecycleQuery;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferRepository;
import com.numobiz.solutions.fornituras.modules.transfers.service.TransferLifecycleQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferLifecycleQueryTest {

	@Mock
	private AssignmentLifecycleQuery assignmentLifecycle;

	@Mock
	private TransferRepository transferRepository;

	@InjectMocks
	private TransferLifecycleQuery query;

	@Test
	void hasOngoingTransfer_delegatesToTransferRepository() {
		when(transferRepository.existsOngoingByEquipmentId(5L)).thenReturn(true);
		assertTrue(query.hasOngoingTransfer(5L));
	}

	@Test
	void hasActiveAssignment_delegatesToAssignmentLifecycle() {
		when(assignmentLifecycle.hasActiveAssignment(5L)).thenReturn(true);
		assertTrue(query.hasActiveAssignment(5L));
	}

	@Test
	void noCommitment_isFalse() {
		when(transferRepository.existsOngoingByEquipmentId(7L)).thenReturn(false);
		assertFalse(query.hasOngoingTransfer(7L));
	}
}
