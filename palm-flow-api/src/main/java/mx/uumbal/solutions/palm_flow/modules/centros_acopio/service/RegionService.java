package mx.uumbal.solutions.palm_flow.modules.centros_acopio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.Region;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper.RegionMapper;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.RegionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    @Transactional(readOnly = true)
    public Page<RegionResponseDTO> getAll(Pageable pageable) {
        return regionRepository.findAll(pageable).map(regionMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public RegionResponseDTO getById(Long id) {
        return regionMapper.toResponseDTO(findById(id));
    }

    @Transactional
    public RegionResponseDTO create(RegionRequestDTO dto) {
        Region region = regionMapper.toEntity(dto);
        region = regionRepository.save(region);
        log.info("Región creada: {} (id: {})", region.getNombre(), region.getId());
        return regionMapper.toResponseDTO(region);
    }

    @Transactional
    public RegionResponseDTO update(Long id, RegionRequestDTO dto) {
        Region region = findById(id);
        region.setNombre(dto.getNombre());
        region = regionRepository.save(region);
        log.info("Región actualizada: {} (id: {})", region.getNombre(), region.getId());
        return regionMapper.toResponseDTO(region);
    }

    @Transactional
    public void delete(Long id) {
        Region region = findById(id);
        regionRepository.delete(region);
        log.info("Región eliminada: {} (id: {})", region.getNombre(), region.getId());
    }

    @Transactional(readOnly = true)
    public Region findById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Región", id));
    }
}
