package mx.uumbal.solutions.palm_flow.modules.productores.service;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.ForbiddenException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import mx.uumbal.solutions.palm_flow.modules.productores.mapper.ProductorMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.ProductorRepository;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductorService {

    private final ProductorRepository productorRepository;
    private final ProductorMapper productorMapper;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public Page<ProductorResponseDTO> getAll(UUID centroAcopioUuid, Pageable pageable) {
        if (centroAcopioUuid != null) {
            scopeService.requireCentroAcopioAccess(centroAcopioUuid);
            return productorRepository
                    .findDistinctByPredioCentroAcopioUuid(centroAcopioUuid, pageable)
                    .map(productorMapper::toResponseDTO);
        }

        Page<Productor> page;
        if (scopeService.hasFullAccess()) {
            page = productorRepository.findAll(pageable);
        } else {
            Set<UUID> allowed = scopeService.getAllowedCentroAcopioUuids();
            page = allowed.isEmpty()
                    ? Page.empty(pageable)
                    : productorRepository.findDistinctByPredioCentroAcopioUuidIn(allowed, pageable);
        }
        return page.map(productorMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ProductorResponseDTO getByUuid(UUID uuid) {
        Productor productor = findByUuid(uuid);
        scopeService.requireProductorAccess(uuid);
        return productorMapper.toResponseDTO(productor);
    }

    @Transactional
    public ProductorResponseDTO create(ProductorRequestDTO dto) {
        Productor productor = productorMapper.toEntity(dto);
        productor = productorRepository.save(productor);
        log.info("Productor creado: {} (uuid: {})", productor.getNombre(), productor.getUuid());
        return productorMapper.toResponseDTO(productor);
    }

    @Transactional
    public ProductorResponseDTO update(UUID uuid, ProductorRequestDTO dto) {
        scopeService.requireProductorAccess(uuid);
        Productor productor = findByUuid(uuid);
        productorMapper.applyRequest(productor, dto);
        productor = productorRepository.save(productor);
        log.info("Productor actualizado: {} (uuid: {})", productor.getNombre(), productor.getUuid());
        return productorMapper.toResponseDTO(productor);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!scopeService.canDeleteProductor(uuid)) {
            throw new ForbiddenException("No tiene permiso para eliminar este productor");
        }
        Productor productor = findByUuid(uuid);
        productorRepository.delete(productor);
        log.info("Productor eliminado: {} (uuid: {})", productor.getNombre(), productor.getUuid());
    }

    @Transactional(readOnly = true)
    public Productor findByUuid(UUID uuid) {
        return productorRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Productor", uuid));
    }
}
