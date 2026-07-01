package com.numobiz.solutions.fornituras.modules.transfers.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferCreateRequest;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferDetail;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferItemDetail;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferSummary;
import com.numobiz.solutions.fornituras.modules.transfers.entity.Transfer;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferItem;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferItemRepository;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de traslados entre almacenes. La consistencia de estado es la garantía central: crear un
 * traslado exige que cada fornitura esté <b>disponible</b> y <b>en el almacén origen</b>, y las pasa
 * a <b>en traslado</b> (bloqueadas para asignación/baja) en una sola transacción; recibir las libera
 * en el destino; cancelar las devuelve al origen. Toda mutación queda auditada (sin PII, por id).
 */
@Service
@Transactional(readOnly = true)
public class TransferService {

	private final TransferRepository repository;
	private final TransferItemRepository itemRepository;
	private final EquipmentRepository equipmentRepository;
	private final WarehouseRepository warehouseRepository;
	private final UserRepository userRepository;
	private final AuditWriter audit;

	public TransferService(
			TransferRepository repository,
			TransferItemRepository itemRepository,
			EquipmentRepository equipmentRepository,
			WarehouseRepository warehouseRepository,
			UserRepository userRepository,
			AuditWriter audit) {
		this.repository = repository;
		this.itemRepository = itemRepository;
		this.equipmentRepository = equipmentRepository;
		this.warehouseRepository = warehouseRepository;
		this.userRepository = userRepository;
		this.audit = audit;
	}

	public Page<TransferSummary> findAll(Long origenId, Long destinoId, TransferStatus status, Pageable pageable) {
		return repository.findAll(filterBy(origenId, destinoId, status), pageable).map(this::toSummary);
	}

	public TransferDetail findById(Long id) {
		return toDetail(getOrThrow(id));
	}

	@Transactional
	public TransferDetail create(TransferCreateRequest request) {
		if (request.origenId().equals(request.destinoId())) {
			throw new BadRequestException("El almacén origen y destino deben ser distintos.");
		}
		requireWarehouse(request.origenId(), "origen");
		requireActiveWarehouse(request.destinoId());

		Transfer transfer = new Transfer();
		transfer.setOrigenId(request.origenId());
		transfer.setDestinoId(request.destinoId());
		transfer.setStatus(TransferStatus.ENVIADO);
		transfer.setFechaEnvio(LocalDateTime.now());
		transfer.setCreadoPor(currentUserId());
		transfer.setObservaciones(request.observaciones());
		Transfer savedTransfer = repository.save(transfer);

		for (Long equipmentId : request.equipmentIds()) {
			Equipment equipment = requireTransferable(equipmentId, request.origenId());
			equipment.setStatus(EquipmentStatus.EN_TRASLADO);
			equipmentRepository.save(equipment);

			TransferItem item = new TransferItem();
			item.setTransferId(savedTransfer.getId());
			item.setEquipmentId(equipmentId);
			itemRepository.save(item);
		}

		audit.record("CREATE_TRANSFER", savedTransfer.getId());
		return toDetail(savedTransfer);
	}

	@Transactional
	public TransferDetail receive(Long id) {
		Transfer transfer = requireEnviado(id);
		transfer.setStatus(TransferStatus.RECIBIDO);
		transfer.setFechaRecepcion(LocalDateTime.now());
		transfer.setRecibidoPor(currentUserId());
		repository.save(transfer);

		for (TransferItem item : itemRepository.findByTransferId(id)) {
			equipmentRepository.findById(item.getEquipmentId()).ifPresent(equipment -> {
				equipment.setStatus(EquipmentStatus.DISPONIBLE);
				equipment.setWarehouseId(transfer.getDestinoId());
				equipmentRepository.save(equipment);
			});
		}

		audit.record("RECEIVE_TRANSFER", id);
		return toDetail(transfer);
	}

	@Transactional
	public TransferDetail cancel(Long id) {
		Transfer transfer = requireEnviado(id);
		transfer.setStatus(TransferStatus.CANCELADO);
		repository.save(transfer);

		// Las fornituras vuelven a disponible en el origen (no cambia su almacén).
		for (TransferItem item : itemRepository.findByTransferId(id)) {
			equipmentRepository.findById(item.getEquipmentId()).ifPresent(equipment -> {
				equipment.setStatus(EquipmentStatus.DISPONIBLE);
				equipmentRepository.save(equipment);
			});
		}

		audit.record("CANCEL_TRANSFER", id);
		return toDetail(transfer);
	}

	/** La fornitura debe existir, estar disponible y encontrarse en el almacén origen. */
	private Equipment requireTransferable(Long equipmentId, Long origenId) {
		Equipment equipment = equipmentRepository.findById(equipmentId)
				.orElseThrow(() -> new NotFoundException("Fornitura no encontrada: " + equipmentId));
		if (equipment.getStatus() != EquipmentStatus.DISPONIBLE) {
			throw new ConflictException(
					"La fornitura " + equipment.getCodigoQr() + " no está disponible (estado: " + equipment.getStatus() + ").");
		}
		if (!equipment.getWarehouseId().equals(origenId)) {
			throw new ConflictException(
					"La fornitura " + equipment.getCodigoQr() + " no está en el almacén origen.");
		}
		return equipment;
	}

	private Transfer requireEnviado(Long id) {
		Transfer transfer = getOrThrow(id);
		if (transfer.getStatus() != TransferStatus.ENVIADO) {
			throw new ConflictException("El traslado no está en curso (estado: " + transfer.getStatus() + ").");
		}
		return transfer;
	}

	private Warehouse requireWarehouse(Long id, String rol) {
		return warehouseRepository.findById(id)
				.orElseThrow(() -> new BadRequestException("Almacén " + rol + " no encontrado: " + id));
	}

	private void requireActiveWarehouse(Long id) {
		Warehouse warehouse = requireWarehouse(id, "destino");
		if (!warehouse.isActive()) {
			throw new BadRequestException("El almacén destino está inactivo.");
		}
	}

	private Transfer getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Traslado no encontrado: " + id));
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
	}

	private Specification<Transfer> filterBy(Long origenId, Long destinoId, TransferStatus status) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (origenId != null) {
				predicates.add(cb.equal(root.get("origenId"), origenId));
			}
			if (destinoId != null) {
				predicates.add(cb.equal(root.get("destinoId"), destinoId));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private TransferSummary toSummary(Transfer transfer) {
		return new TransferSummary(
				transfer.getId(),
				transfer.getOrigenId(),
				warehouseName(transfer.getOrigenId()),
				transfer.getDestinoId(),
				warehouseName(transfer.getDestinoId()),
				transfer.getStatus(),
				transfer.getFechaEnvio(),
				transfer.getFechaRecepcion(),
				itemRepository.countByTransferId(transfer.getId()));
	}

	private TransferDetail toDetail(Transfer transfer) {
		List<TransferItemDetail> items = new ArrayList<>();
		for (TransferItem item : itemRepository.findByTransferId(transfer.getId())) {
			Equipment equipment = equipmentRepository.findById(item.getEquipmentId()).orElse(null);
			items.add(new TransferItemDetail(
					item.getEquipmentId(),
					equipment == null ? null : equipment.getCodigoQr(),
					equipment == null ? null : equipment.getDescripcion()));
		}
		return new TransferDetail(
				transfer.getId(),
				transfer.getOrigenId(),
				warehouseName(transfer.getOrigenId()),
				transfer.getDestinoId(),
				warehouseName(transfer.getDestinoId()),
				transfer.getStatus(),
				transfer.getFechaEnvio(),
				transfer.getFechaRecepcion(),
				transfer.getObservaciones(),
				items);
	}

	private String warehouseName(Long id) {
		return warehouseRepository.findById(id).map(Warehouse::getNombre).orElse(null);
	}
}
