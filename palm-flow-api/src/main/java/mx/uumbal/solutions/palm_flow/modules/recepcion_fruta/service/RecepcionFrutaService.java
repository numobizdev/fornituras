package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.CentroAcopioService;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.CentroAcopioPrecioKgService;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import mx.uumbal.solutions.palm_flow.modules.productores.service.LoteService;
import mx.uumbal.solutions.palm_flow.modules.productores.service.PredioService;
import mx.uumbal.solutions.palm_flow.modules.productores.service.ProductorService;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.mapper.RecepcionFrutaMapper;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository.RecepcionFrutaRepository;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionFrutaService {

    private final RecepcionFrutaRepository recepcionFrutaRepository;
    private final RecepcionFrutaMapper recepcionFrutaMapper;
    private final FolioGeneratorService folioGeneratorService;
    private final CentroAcopioService centroAcopioService;
    private final CentroAcopioPrecioKgService precioKgService;
    private final ProductorService productorService;
    private final PredioService predioService;
    private final LoteService loteService;
    private final UserRepository userRepository;
    private final CentroAcopioScopeService scopeService;
    private final RecepcionFrutaEmailService recepcionFrutaEmailService;

    @Transactional(readOnly = true)
    public Page<RecepcionFrutaResponseDTO> getAll(UUID centroAcopioUuid, UUID productorUuid, Pageable pageable) {
        if (!scopeService.hasFullAccess()) {
            Set<UUID> allowed = scopeService.getAllowedCentroAcopioUuids();
            if (allowed.isEmpty()) {
                return Page.empty(pageable);
            }
            if (centroAcopioUuid != null) {
                scopeService.requireCentroAcopioAccess(centroAcopioUuid);
            }
            if (productorUuid != null) {
                scopeService.requireProductorAccess(productorUuid);
            }
            Page<RecepcionFruta> page;
            if (centroAcopioUuid != null && productorUuid != null) {
                page = recepcionFrutaRepository.findByCentroAcopioUuidInAndProductorUuid(
                        Set.of(centroAcopioUuid), productorUuid, pageable);
            } else if (centroAcopioUuid != null) {
                page = recepcionFrutaRepository.findByCentroAcopioUuid(centroAcopioUuid, pageable);
            } else if (productorUuid != null) {
                page = recepcionFrutaRepository.findByCentroAcopioUuidInAndProductorUuid(
                        allowed, productorUuid, pageable);
            } else {
                page = recepcionFrutaRepository.findByCentroAcopioUuidIn(allowed, pageable);
            }
            return page.map(recepcionFrutaMapper::toResponseDTO);
        }

        Page<RecepcionFruta> page;
        if (centroAcopioUuid != null) {
            page = recepcionFrutaRepository.findByCentroAcopioUuid(centroAcopioUuid, pageable);
        } else if (productorUuid != null) {
            page = recepcionFrutaRepository.findByProductorUuid(productorUuid, pageable);
        } else {
            page = recepcionFrutaRepository.findAll(pageable);
        }
        return page.map(recepcionFrutaMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public RecepcionFrutaResponseDTO getByUuid(UUID uuid) {
        RecepcionFruta recepcion = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(recepcion.getCentroAcopio().getUuid());
        return recepcionFrutaMapper.toResponseDTO(recepcion);
    }

    @Transactional
    public RecepcionFrutaResponseDTO create(RecepcionFrutaRequestDTO dto) {
        validateWriteAccess(dto);
        RecepcionFruta recepcion = new RecepcionFruta();
        recepcion.setFolio(folioGeneratorService.generateFolio());
        applyRelations(recepcion, dto);
        recepcionFrutaMapper.applyRequest(recepcion, dto);
        applyPricing(recepcion, dto);
        if (recepcion.isRegistroOffline() && recepcion.getFechaSync() == null) {
            recepcion.setFechaSync(Instant.now());
        }
        recepcion = recepcionFrutaRepository.save(recepcion);
        log.info("Recepción de fruta creada: {} (uuid: {})", recepcion.getFolio(), recepcion.getUuid());
        recepcionFrutaEmailService.notifyProductorIfEmailPresent(recepcion);
        return recepcionFrutaMapper.toResponseDTO(recepcion);
    }

    @Transactional
    public RecepcionFrutaResponseDTO update(UUID uuid, RecepcionFrutaRequestDTO dto) {
        RecepcionFruta recepcion = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(recepcion.getCentroAcopio().getUuid());
        validateWriteAccess(dto);
        applyRelations(recepcion, dto);
        recepcionFrutaMapper.applyRequest(recepcion, dto);
        applyPricing(recepcion, dto);
        recepcion = recepcionFrutaRepository.save(recepcion);
        log.info("Recepción de fruta actualizada: {} (uuid: {})", recepcion.getFolio(), recepcion.getUuid());
        return recepcionFrutaMapper.toResponseDTO(recepcion);
    }

    @Transactional
    public RecepcionFrutaResponseDTO markSynced(UUID uuid) {
        RecepcionFruta recepcion = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(recepcion.getCentroAcopio().getUuid());
        recepcion.setFechaSync(Instant.now());
        recepcion = recepcionFrutaRepository.save(recepcion);
        log.info("Recepción de fruta sincronizada: {} (uuid: {})", recepcion.getFolio(), recepcion.getUuid());
        return recepcionFrutaMapper.toResponseDTO(recepcion);
    }

    @Transactional
    public void delete(UUID uuid) {
        RecepcionFruta recepcion = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(recepcion.getCentroAcopio().getUuid());
        recepcionFrutaRepository.delete(recepcion);
        log.info("Recepción de fruta eliminada: {} (uuid: {})", recepcion.getFolio(), recepcion.getUuid());
    }

    @Transactional(readOnly = true)
    public RecepcionFruta findByUuid(UUID uuid) {
        return recepcionFrutaRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Recepción de fruta", uuid));
    }

    private void validateWriteAccess(RecepcionFrutaRequestDTO dto) {
        scopeService.requireCentroAcopioAccess(dto.getCentroAcopioUuid());
        scopeService.requireProductorAccess(dto.getProductorUuid());
        validatePredio(dto);
    }

    private void validatePredio(RecepcionFrutaRequestDTO dto) {
        Predio predio = predioService.findByUuid(dto.getPredioUuid());
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        if (!predio.getProductor().getUuid().equals(dto.getProductorUuid())) {
            throw new BadRequestException("El predio no pertenece al productor indicado");
        }
        if (!predio.getCentroAcopio().getUuid().equals(dto.getCentroAcopioUuid())) {
            throw new BadRequestException("El predio no pertenece al centro de acopio indicado");
        }
        if (dto.getLoteUuid() != null) {
            var lote = loteService.findByUuid(dto.getLoteUuid());
            if (!lote.getPredio().getUuid().equals(dto.getPredioUuid())) {
                throw new BadRequestException("El lote no pertenece al predio indicado");
            }
        }
    }

    private void applyRelations(RecepcionFruta recepcion, RecepcionFrutaRequestDTO dto) {
        CentroAcopio centroAcopio = centroAcopioService.findByUuid(dto.getCentroAcopioUuid());
        Productor productor = productorService.findByUuid(dto.getProductorUuid());
        Predio predio = predioService.findByUuid(dto.getPredioUuid());
        User usuario = userRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("User", dto.getUsuarioId()));
        recepcion.setCentroAcopio(centroAcopio);
        recepcion.setProductor(productor);
        recepcion.setPredio(predio);
        recepcion.setLote(dto.getLoteUuid() != null ? loteService.findByUuid(dto.getLoteUuid()) : null);
        recepcion.setUsuario(usuario);
    }

    private void applyPricing(RecepcionFruta recepcion, RecepcionFrutaRequestDTO dto) {
        try {
            var precio = precioKgService.findEffectivePriceForInstant(dto.getCentroAcopioUuid(), dto.getFecha());
            recepcionFrutaMapper.applyPricing(recepcion, precio.getPrecioKg());
        } catch (NotFoundException e) {
            throw new BadRequestException("No hay precio vigente para este centro en la fecha indicada");
        }
    }
}
