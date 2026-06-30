package mx.uumbal.solutions.palm_flow.modules.productores.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.CentroAcopioService;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.ComunidadService;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.EstadoService;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.MunicipioService;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteSummaryDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import mx.uumbal.solutions.palm_flow.modules.productores.mapper.LoteMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.mapper.PredioMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.LoteRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.PredioRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.util.FiscalIdNormalizer;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredioService {

    private final PredioRepository predioRepository;
    private final LoteRepository loteRepository;
    private final PredioMapper predioMapper;
    private final LoteMapper loteMapper;
    private final ProductorService productorService;
    private final CentroAcopioService centroAcopioService;
    private final EstadoService estadoService;
    private final MunicipioService municipioService;
    private final ComunidadService comunidadService;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public Page<PredioResponseDTO> getAll(UUID productorUuid, boolean includeLotes, Pageable pageable) {
        Page<Predio> page;
        if (scopeService.hasFullAccess()) {
            page = productorUuid != null
                    ? predioRepository.findByProductorUuid(productorUuid, pageable)
                    : predioRepository.findAll(pageable);
        } else {
            Set<UUID> allowed = scopeService.getAllowedCentroAcopioUuids();
            if (allowed.isEmpty()) {
                page = Page.empty(pageable);
            } else if (productorUuid != null) {
                page = predioRepository.findByProductorUuidAndCentroAcopioUuidIn(productorUuid, allowed, pageable);
            } else {
                page = predioRepository.findByCentroAcopioUuidIn(allowed, pageable);
            }
        }
        if (!includeLotes) {
            return page.map(predioMapper::toResponseDTO);
        }
        Map<UUID, List<LoteSummaryDTO>> lotesByPredio = loadLoteSummaries(
                page.getContent().stream().map(Predio::getUuid).toList());
        return page.map(predio -> {
            PredioResponseDTO dto = predioMapper.toResponseDTO(predio);
            dto.setLotes(lotesByPredio.getOrDefault(predio.getUuid(), List.of()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public PredioResponseDTO getByUuid(UUID uuid, boolean includeLotes) {
        Predio predio = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        PredioResponseDTO dto = predioMapper.toResponseDTO(predio);
        if (includeLotes) {
            dto.setLotes(loteRepository.findByPredioUuid(uuid, Pageable.unpaged()).getContent().stream()
                    .map(loteMapper::toSummaryDTO)
                    .toList());
        }
        return dto;
    }

    @Transactional
    public PredioResponseDTO create(PredioRequestDTO dto) {
        scopeService.requireCentroAcopioAccess(dto.getCentroAcopioUuid());
        validateUniqueFiscalId(dto.getFiscalId(), null);
        Predio predio = new Predio();
        applyRelations(predio, dto);
        predioMapper.applyRequest(predio, dto);
        predio = predioRepository.save(predio);
        log.info("Predio creado: {} (uuid: {})", predio.getNombre(), predio.getUuid());
        return predioMapper.toResponseDTO(predio);
    }

    @Transactional
    public PredioResponseDTO update(UUID uuid, PredioRequestDTO dto) {
        Predio predio = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        scopeService.requireCentroAcopioAccess(dto.getCentroAcopioUuid());
        validateUniqueFiscalId(dto.getFiscalId(), uuid);
        applyRelations(predio, dto);
        predioMapper.applyRequest(predio, dto);
        predio = predioRepository.save(predio);
        log.info("Predio actualizado: {} (uuid: {})", predio.getNombre(), predio.getUuid());
        return predioMapper.toResponseDTO(predio);
    }

    @Transactional
    public void delete(UUID uuid) {
        Predio predio = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        predioRepository.delete(predio);
        log.info("Predio eliminado: {} (uuid: {})", predio.getNombre(), predio.getUuid());
    }

    @Transactional(readOnly = true)
    public Predio findByUuid(UUID uuid) {
        return predioRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Predio", uuid));
    }

    private Map<UUID, List<LoteSummaryDTO>> loadLoteSummaries(List<UUID> predioUuids) {
        if (predioUuids.isEmpty()) {
            return Map.of();
        }
        return loteRepository.findByPredioUuidIn(predioUuids).stream()
                .collect(Collectors.groupingBy(
                        lote -> lote.getPredio().getUuid(),
                        Collectors.mapping(loteMapper::toSummaryDTO, Collectors.toList())));
    }

    private void applyRelations(Predio predio, PredioRequestDTO dto) {
        Productor productor = productorService.findByUuid(dto.getProductorUuid());
        CentroAcopio centroAcopio = centroAcopioService.findByUuid(dto.getCentroAcopioUuid());
        Estado estado = dto.getEstadoId() != null ? estadoService.findById(dto.getEstadoId()) : null;
        Municipio municipio = dto.getMunicipioId() != null ? municipioService.findById(dto.getMunicipioId()) : null;
        Comunidad comunidad = dto.getComunidadId() != null ? comunidadService.findById(dto.getComunidadId()) : null;
        validateGeografia(estado, municipio, comunidad);
        predio.setProductor(productor);
        predio.setCentroAcopio(centroAcopio);
        predio.setEstado(estado);
        predio.setMunicipio(municipio);
        predio.setComunidad(comunidad);
    }

    private void validateUniqueFiscalId(String fiscalId, UUID excludeUuid) {
        String normalized = FiscalIdNormalizer.normalize(fiscalId);
        if (normalized == null) {
            return;
        }
        if (excludeUuid == null) {
            if (predioRepository.findByFiscalIdIgnoreCase(normalized).isPresent()) {
                throw new BadRequestException("Ya existe un predio con fiscal_id: " + normalized);
            }
            return;
        }
        if (predioRepository.existsByFiscalIdIgnoreCaseAndUuidNot(normalized, excludeUuid)) {
            throw new BadRequestException("Ya existe un predio con fiscal_id: " + normalized);
        }
    }

    private void validateGeografia(Estado estado, Municipio municipio, Comunidad comunidad) {
        if (municipio != null && estado != null
                && !municipio.getEstado().getId().equals(estado.getId())) {
            throw new BadRequestException("El municipio no pertenece al estado indicado");
        }
        if (comunidad != null && municipio != null
                && !comunidad.getMunicipio().getId().equals(municipio.getId())) {
            throw new BadRequestException("La comunidad no pertenece al municipio indicado");
        }
        if (comunidad != null && estado == null) {
            throw new BadRequestException("El estado es requerido cuando se indica comunidad");
        }
        if (municipio != null && estado == null) {
            throw new BadRequestException("El estado es requerido cuando se indica municipio");
        }
    }
}
