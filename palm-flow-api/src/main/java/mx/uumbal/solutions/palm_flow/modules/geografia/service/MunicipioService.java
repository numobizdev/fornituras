package mx.uumbal.solutions.palm_flow.modules.geografia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.mapper.MunicipioMapper;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.MunicipioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MunicipioService {

    private final MunicipioRepository municipioRepository;
    private final MunicipioMapper municipioMapper;
    private final EstadoService estadoService;

    @Transactional(readOnly = true)
    public Page<MunicipioResponseDTO> getAll(Long estadoId, Pageable pageable) {
        Page<Municipio> page = estadoId != null
                ? municipioRepository.findByEstadoId(estadoId, pageable)
                : municipioRepository.findAll(pageable);
        return page.map(municipioMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public MunicipioResponseDTO getById(Long id) {
        return municipioMapper.toResponseDTO(findById(id));
    }

    @Transactional
    public MunicipioResponseDTO create(MunicipioRequestDTO dto) {
        Estado estado = estadoService.findById(dto.getEstadoId());
        Municipio municipio = new Municipio();
        municipio.setNombre(dto.getNombre());
        municipio.setEstado(estado);
        municipio = municipioRepository.save(municipio);
        log.info("Municipio creado: {} (id: {})", municipio.getNombre(), municipio.getId());
        return municipioMapper.toResponseDTO(municipio);
    }

    @Transactional
    public MunicipioResponseDTO update(Long id, MunicipioRequestDTO dto) {
        Municipio municipio = findById(id);
        Estado estado = estadoService.findById(dto.getEstadoId());
        municipioMapper.updateEntity(municipio, dto);
        municipio.setEstado(estado);
        municipio = municipioRepository.save(municipio);
        log.info("Municipio actualizado: {} (id: {})", municipio.getNombre(), municipio.getId());
        return municipioMapper.toResponseDTO(municipio);
    }

    @Transactional
    public void delete(Long id) {
        Municipio municipio = findById(id);
        municipioRepository.delete(municipio);
        log.info("Municipio eliminado: {} (id: {})", municipio.getNombre(), municipio.getId());
    }

    @Transactional(readOnly = true)
    public Municipio findById(Long id) {
        return municipioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Municipio", id));
    }
}
