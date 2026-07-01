package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferCreateRequest;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferDetail;
import com.numobiz.solutions.fornituras.modules.transfers.entity.Transfer;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferItem;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferItemRepository;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferRepository;
import com.numobiz.solutions.fornituras.modules.transfers.service.TransferService;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

	@Mock private TransferRepository repository;
	@Mock private TransferItemRepository itemRepository;
	@Mock private EquipmentRepository equipmentRepository;
	@Mock private WarehouseRepository warehouseRepository;
	@Mock private UserRepository userRepository;
	@Mock private AuditWriter audit;

	@InjectMocks private TransferService service;

	private static final Long ORIGEN = 1L;
	private static final Long DESTINO = 2L;

	@Test
	void create_movesAvailableEquipmentIntoTransit() {
		stubWarehouses(true);
		Equipment e1 = equipment(10L, EquipmentStatus.DISPONIBLE, ORIGEN);
		Equipment e2 = equipment(11L, EquipmentStatus.DISPONIBLE, ORIGEN);
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e1));
		when(equipmentRepository.findById(11L)).thenReturn(Optional.of(e2));
		stubTransferSave(100L);
		when(itemRepository.findByTransferId(100L)).thenReturn(List.of());

		TransferDetail detail = service.create(new TransferCreateRequest(ORIGEN, DESTINO, List.of(10L, 11L), null));

		assertEquals(TransferStatus.ENVIADO, detail.status());
		assertEquals(EquipmentStatus.EN_TRASLADO, e1.getStatus());
		assertEquals(EquipmentStatus.EN_TRASLADO, e2.getStatus());
		verify(audit).record(eq("CREATE_TRANSFER"), eq(100L));
	}

	@Test
	void create_rejectsSameOrigenAndDestino() {
		assertThrows(BadRequestException.class,
				() -> service.create(new TransferCreateRequest(ORIGEN, ORIGEN, List.of(10L), null)));
	}

	@Test
	void create_rejectsUnavailableEquipment() {
		stubWarehouses(true);
		stubTransferSave(100L);
		when(equipmentRepository.findById(10L))
				.thenReturn(Optional.of(equipment(10L, EquipmentStatus.ASIGNADA, ORIGEN)));

		assertThrows(ConflictException.class,
				() -> service.create(new TransferCreateRequest(ORIGEN, DESTINO, List.of(10L), null)));
	}

	@Test
	void create_rejectsEquipmentNotInOrigen() {
		stubWarehouses(true);
		stubTransferSave(100L);
		when(equipmentRepository.findById(10L))
				.thenReturn(Optional.of(equipment(10L, EquipmentStatus.DISPONIBLE, 99L)));

		assertThrows(ConflictException.class,
				() -> service.create(new TransferCreateRequest(ORIGEN, DESTINO, List.of(10L), null)));
	}

	@Test
	void receive_releasesEquipmentIntoDestino() {
		Transfer transfer = transfer(50L, TransferStatus.ENVIADO);
		when(repository.findById(50L)).thenReturn(Optional.of(transfer));
		when(repository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
		TransferItem item = item(50L, 10L);
		when(itemRepository.findByTransferId(50L)).thenReturn(List.of(item));
		Equipment e = equipment(10L, EquipmentStatus.EN_TRASLADO, ORIGEN);
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
		stubWarehouses(true);

		TransferDetail detail = service.receive(50L);

		assertEquals(TransferStatus.RECIBIDO, detail.status());
		assertEquals(EquipmentStatus.DISPONIBLE, e.getStatus());
		assertEquals(DESTINO, e.getWarehouseId());
		verify(audit).record(eq("RECEIVE_TRANSFER"), eq(50L));
	}

	@Test
	void receive_rejectsNonOngoingTransfer() {
		when(repository.findById(50L)).thenReturn(Optional.of(transfer(50L, TransferStatus.RECIBIDO)));
		assertThrows(ConflictException.class, () -> service.receive(50L));
	}

	@Test
	void cancel_returnsEquipmentToOrigen() {
		Transfer transfer = transfer(60L, TransferStatus.ENVIADO);
		when(repository.findById(60L)).thenReturn(Optional.of(transfer));
		when(repository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
		when(itemRepository.findByTransferId(60L)).thenReturn(List.of(item(60L, 10L)));
		Equipment e = equipment(10L, EquipmentStatus.EN_TRASLADO, ORIGEN);
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
		stubWarehouses(true);

		TransferDetail detail = service.cancel(60L);

		assertEquals(TransferStatus.CANCELADO, detail.status());
		assertEquals(EquipmentStatus.DISPONIBLE, e.getStatus());
		assertEquals(ORIGEN, e.getWarehouseId()); // no cambia de almacén al cancelar
		verify(audit).record(eq("CANCEL_TRANSFER"), eq(60L));
	}

	private void stubWarehouses(boolean active) {
		Warehouse w = warehouse(active); // construir el mock antes del when() externo (evita stubbing anidado)
		when(warehouseRepository.findById(anyLong())).thenReturn(Optional.of(w));
	}

	private void stubTransferSave(Long id) {
		when(repository.save(any(Transfer.class))).thenAnswer(inv -> {
			Transfer t = inv.getArgument(0);
			if (t.getId() == null) {
				t.setId(id);
			}
			return t;
		});
	}

	private Warehouse warehouse(boolean active) {
		Warehouse w = mock(Warehouse.class);
		lenient().when(w.isActive()).thenReturn(active);
		lenient().when(w.getNombre()).thenReturn("Almacén");
		return w;
	}

	private Equipment equipment(Long id, EquipmentStatus status, Long warehouseId) {
		Equipment e = new Equipment();
		e.setId(id);
		e.setStatus(status);
		e.setWarehouseId(warehouseId);
		e.setCodigoQr("FOR-" + id);
		e.setCodigoNormalizado("FOR" + id);
		return e;
	}

	private Transfer transfer(Long id, TransferStatus status) {
		Transfer t = new Transfer();
		t.setId(id);
		t.setStatus(status);
		t.setOrigenId(ORIGEN);
		t.setDestinoId(DESTINO);
		return t;
	}

	private TransferItem item(Long transferId, Long equipmentId) {
		TransferItem item = new TransferItem();
		item.setTransferId(transferId);
		item.setEquipmentId(equipmentId);
		return item;
	}
}
