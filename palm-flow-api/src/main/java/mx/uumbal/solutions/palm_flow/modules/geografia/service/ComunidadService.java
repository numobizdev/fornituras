package mx.uumbal.solutions.palm_flow.modules.geografia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.mapper.ComunidadMapper;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.ComunidadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComunidadService {

    private final ComunidadRepository comunidadRepository;
    private final ComunidadMapper comunidadMapper;
    private final MunicipioService municipioService;

    @Transactional(readOnly = true)
    public Page<ComunidadResponseDTO> getAll(Long municipioId, Pageable pageable) {
        Page<Comunidad> page = municipioId != null
                ? comunidadRepository.findByMunicipioId(municipioId, pageable)
                : comunidadRepository.findAll(pageable);
        return page.map(comunidadMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ComunidadResponseDTO getById(Long id) {
        return comunidadMapper.toResponseDTO(findById(id));
    }

    @Transactional
    public ComunidadResponseDTO create(ComunidadRequestDTO dto) {
        Municipio municipio = municipioService.findById(dto.getMunicipioId());
        Comunidad comunidad = new Comunidad();
        comunidad.setNombre(dto.getNombre());
        comunidad.setMunicipio(municipio);
        comunidad = comunidadRepository.save(comunidad);
        log.info("Comunidad creada: {} (id: {})", comunidad.getNombre(), comunidad.getId());
        return comunidadMapper.toResponseDTO(comunidad);
    }

    @Transactional
    public ComunidadResponseDTO update(Long id, ComunidadRequestDTO dto) {
        Comunidad comunidad = findById(id);
        Municipio municipio = municipioService.findById(dto.getMunicipioId());
        comunidadMapper.updateEntity(comunidad, dto);
        comunidad.setMunicipio(municipio);
        comunidad = comunidadRepository.save(comunidad);
        log.info("Comunidad actualizada: {} (id: {})", comunidad.getNombre(), comunidad.getId());
        return comunidadMapper.toResponseDTO(comunidad);
    }

    @Transactional
    public void delete(Long id) {
        Comunidad comunidad = findById(id);
        comunidadRepository.delete(comunidad);
        log.info("Comunidad eliminada: {} (id: {})", comunidad.getNombre(), comunidad.getId());
    }

    @Transactional(readOnly = true)
    public Comunidad findById(Long id) {
        return comunidadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comunidad", id));
    }
}
