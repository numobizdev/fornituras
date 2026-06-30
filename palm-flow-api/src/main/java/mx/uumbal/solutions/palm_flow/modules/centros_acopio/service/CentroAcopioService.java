package mx.uumbal.solutions.palm_flow.modules.centros_acopio.service;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.ForbiddenException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper.CentroAcopioMapper;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.ComunidadService;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.EstadoService;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.MunicipioService;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentroAcopioService {

    private final CentroAcopioRepository centroAcopioRepository;
    private final CentroAcopioMapper centroAcopioMapper;
    private final RegionService regionService;
    private final EstadoService estadoService;
    private final MunicipioService municipioService;
    private final ComunidadService comunidadService;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public Page<CentroAcopioResponseDTO> getAll(Long regionId, Pageable pageable) {
        Page<CentroAcopio> page;
        if (scopeService.hasFullAccess()) {
            page = regionId != null
                    ? centroAcopioRepository.findByRegionId(regionId, pageable)
                    : centroAcopioRepository.findAll(pageable);
        } else {
            Set<UUID> allowed = scopeService.getAllowedCentroAcopioUuids();
            if (allowed.isEmpty()) {
                page = Page.empty(pageable);
            } else if (regionId != null) {
                page = centroAcopioRepository.findByRegionIdAndUuidIn(regionId, allowed, pageable);
            } else {
                page = centroAcopioRepository.findByUuidIn(allowed, pageable);
            }
        }
        return page.map(centroAcopioMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CentroAcopioResponseDTO getByUuid(UUID uuid) {
        CentroAcopio centroAcopio = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(uuid);
        return centroAcopioMapper.toResponseDTO(centroAcopio);
    }

    @Transactional
    public CentroAcopioResponseDTO create(CentroAcopioRequestDTO dto) {
        if (!scopeService.hasFullAccess()) {
            throw new ForbiddenException("No tiene permiso para crear centros de acopio");
        }
        CentroAcopio centroAcopio = new CentroAcopio();
        applyRelations(centroAcopio, dto);
        centroAcopioMapper.applyRequest(centroAcopio, dto);
        centroAcopio = centroAcopioRepository.save(centroAcopio);
        log.info("Centro de acopio creado: {} (uuid: {})", centroAcopio.getNombre(), centroAcopio.getUuid());
        return centroAcopioMapper.toResponseDTO(centroAcopio);
    }

    @Transactional
    public CentroAcopioResponseDTO update(UUID uuid, CentroAcopioRequestDTO dto) {
        if (!scopeService.hasFullAccess()) {
            throw new ForbiddenException("No tiene permiso para actualizar centros de acopio");
        }
        CentroAcopio centroAcopio = findByUuid(uuid);
        applyRelations(centroAcopio, dto);
        centroAcopioMapper.applyRequest(centroAcopio, dto);
        centroAcopio = centroAcopioRepository.save(centroAcopio);
        log.info("Centro de acopio actualizado: {} (uuid: {})", centroAcopio.getNombre(), centroAcopio.getUuid());
        return centroAcopioMapper.toResponseDTO(centroAcopio);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!scopeService.hasFullAccess()) {
            throw new ForbiddenException("No tiene permiso para eliminar centros de acopio");
        }
        CentroAcopio centroAcopio = findByUuid(uuid);
        centroAcopioRepository.delete(centroAcopio);
        log.info("Centro de acopio eliminado: {} (uuid: {})", centroAcopio.getNombre(), centroAcopio.getUuid());
    }

    @Transactional(readOnly = true)
    public CentroAcopio findByUuid(UUID uuid) {
        return centroAcopioRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Centro de acopio", uuid));
    }

    private void applyRelations(CentroAcopio centroAcopio, CentroAcopioRequestDTO dto) {
        centroAcopio.setRegion(regionService.findById(dto.getRegionId()));
        Estado estado = estadoService.findById(dto.getEstadoId());
        Municipio municipio = municipioService.findById(dto.getMunicipioId());
        Comunidad comunidad = comunidadService.findById(dto.getComunidadId());
        validateGeografia(estado, municipio, comunidad);
        centroAcopio.setEstado(estado);
        centroAcopio.setMunicipio(municipio);
        centroAcopio.setComunidad(comunidad);
    }

    private void validateGeografia(Estado estado, Municipio municipio, Comunidad comunidad) {
        if (!municipio.getEstado().getId().equals(estado.getId())) {
            throw new BadRequestException("El municipio no pertenece al estado indicado");
        }
        if (!comunidad.getMunicipio().getId().equals(municipio.getId())) {
            throw new BadRequestException("La comunidad no pertenece al municipio indicado");
        }
    }
}
