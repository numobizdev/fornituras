package mx.uumbal.solutions.palm_flow.modules.centros_acopio.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.ForbiddenException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioPrecioKg;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper.CentroAcopioPrecioKgMapper;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioPrecioKgRepository;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentroAcopioPrecioKgService {

    private final CentroAcopioPrecioKgRepository precioKgRepository;
    private final CentroAcopioPrecioKgMapper precioKgMapper;
    private final CentroAcopioService centroAcopioService;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public Page<CentroAcopioPrecioKgResponseDTO> getHistory(UUID centroUuid, Pageable pageable) {
        scopeService.requireCentroAcopioAccess(centroUuid);
        centroAcopioService.findByUuid(centroUuid);
        return precioKgRepository.findByCentroAcopioUuid(centroUuid, pageable)
                .map(precioKgMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CentroAcopioPrecioKgResponseDTO getVigente(UUID centroUuid, LocalDate fecha) {
        scopeService.requireCentroAcopioAccess(centroUuid);
        centroAcopioService.findByUuid(centroUuid);
        LocalDate referenceDate = fecha != null ? fecha : LocalDate.now(ZoneOffset.UTC);
        CentroAcopioPrecioKg precio = findEffectivePrice(centroUuid, referenceDate);
        return precioKgMapper.toResponseDTO(precio);
    }

    @Transactional(readOnly = true)
    public CentroAcopioPrecioKg findEffectivePrice(UUID centroUuid, LocalDate fecha) {
        return precioKgRepository
                .findFirstByCentroAcopioUuidAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(centroUuid, fecha)
                .orElseThrow(() -> new NotFoundException(
                        "Precio vigente para centro de acopio " + centroUuid + " en fecha " + fecha));
    }

    @Transactional(readOnly = true)
    public CentroAcopioPrecioKg findEffectivePriceForInstant(UUID centroUuid, Instant instant) {
        LocalDate fecha = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return findEffectivePrice(centroUuid, fecha);
    }

    @Transactional
    public CentroAcopioPrecioKgResponseDTO create(UUID centroUuid, CentroAcopioPrecioKgRequestDTO dto) {
        if (!scopeService.hasFullAccess()) {
            throw new ForbiddenException("No tiene permiso para registrar precios");
        }
        CentroAcopio centro = centroAcopioService.findByUuid(centroUuid);
        if (precioKgRepository.existsByCentroAcopioUuidAndFechaVigencia(centroUuid, dto.getFechaVigencia())) {
            throw new BadRequestException("Ya existe un precio con la misma fecha de vigencia para este centro");
        }
        CentroAcopioPrecioKg precio = new CentroAcopioPrecioKg();
        precio.setCentroAcopio(centro);
        precio.setPrecioKg(dto.getPrecioKg());
        precio.setFechaVigencia(dto.getFechaVigencia());
        precio = precioKgRepository.save(precio);
        log.info("Precio kg registrado: {} para centro {} (vigencia: {})",
                precio.getPrecioKg(), centro.getNombre(), precio.getFechaVigencia());
        return precioKgMapper.toResponseDTO(precio);
    }
}
