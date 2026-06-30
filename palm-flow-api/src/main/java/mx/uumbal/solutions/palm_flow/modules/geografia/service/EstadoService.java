package mx.uumbal.solutions.palm_flow.modules.geografia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.mapper.EstadoMapper;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.EstadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstadoService {

    private final EstadoRepository estadoRepository;
    private final EstadoMapper estadoMapper;

    @Transactional(readOnly = true)
    public Page<EstadoResponseDTO> getAll(Pageable pageable) {
        return estadoRepository.findAll(pageable).map(estadoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public EstadoResponseDTO getById(Long id) {
        return estadoMapper.toResponseDTO(findById(id));
    }

    @Transactional
    public EstadoResponseDTO create(EstadoRequestDTO dto) {
        Estado estado = estadoMapper.toEntity(dto);
        estado = estadoRepository.save(estado);
        log.info("Estado creado: {} (id: {})", estado.getNombre(), estado.getId());
        return estadoMapper.toResponseDTO(estado);
    }

    @Transactional
    public EstadoResponseDTO update(Long id, EstadoRequestDTO dto) {
        Estado estado = findById(id);
        estado.setNombre(dto.getNombre());
        estado = estadoRepository.save(estado);
        log.info("Estado actualizado: {} (id: {})", estado.getNombre(), estado.getId());
        return estadoMapper.toResponseDTO(estado);
    }

    @Transactional
    public void delete(Long id) {
        Estado estado = findById(id);
        estadoRepository.delete(estado);
        log.info("Estado eliminado: {} (id: {})", estado.getNombre(), estado.getId());
    }

    @Transactional(readOnly = true)
    public Estado findById(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estado", id));
    }
}
