package mx.uumbal.solutions.palm_flow.modules.productores.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.mapper.LoteMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.LoteRepository;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoteService {

    private final LoteRepository loteRepository;
    private final LoteMapper loteMapper;
    private final PredioService predioService;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public Page<LoteResponseDTO> getAll(UUID predioUuid, Pageable pageable) {
        if (predioUuid == null) {
            throw new BadRequestException("El parámetro predioUuid es requerido");
        }
        Predio predio = predioService.findByUuid(predioUuid);
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        return loteRepository.findByPredioUuid(predioUuid, pageable).map(loteMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public LoteResponseDTO getByUuid(UUID uuid) {
        Lote lote = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(lote.getPredio().getCentroAcopio().getUuid());
        return loteMapper.toResponseDTO(lote);
    }

    @Transactional
    public LoteResponseDTO create(LoteRequestDTO dto) {
        Predio predio = predioService.findByUuid(dto.getPredioUuid());
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        Lote lote = new Lote();
        lote.setPredio(predio);
        loteMapper.applyRequest(lote, dto);
        lote = loteRepository.save(lote);
        log.info("Lote creado: {} (uuid: {})", lote.getNombre(), lote.getUuid());
        return loteMapper.toResponseDTO(lote);
    }

    @Transactional
    public LoteResponseDTO update(UUID uuid, LoteRequestDTO dto) {
        Lote lote = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(lote.getPredio().getCentroAcopio().getUuid());
        Predio predio = predioService.findByUuid(dto.getPredioUuid());
        scopeService.requireCentroAcopioAccess(predio.getCentroAcopio().getUuid());
        lote.setPredio(predio);
        loteMapper.applyRequest(lote, dto);
        lote = loteRepository.save(lote);
        log.info("Lote actualizado: {} (uuid: {})", lote.getNombre(), lote.getUuid());
        return loteMapper.toResponseDTO(lote);
    }

    @Transactional
    public void delete(UUID uuid) {
        Lote lote = findByUuid(uuid);
        scopeService.requireCentroAcopioAccess(lote.getPredio().getCentroAcopio().getUuid());
        loteRepository.delete(lote);
        log.info("Lote eliminado: {} (uuid: {})", lote.getNombre(), lote.getUuid());
    }

    @Transactional(readOnly = true)
    public Lote findByUuid(UUID uuid) {
        return loteRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Lote", uuid));
    }
}
