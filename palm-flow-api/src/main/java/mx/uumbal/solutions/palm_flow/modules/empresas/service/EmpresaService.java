package mx.uumbal.solutions.palm_flow.modules.empresas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.entity.Empresa;
import mx.uumbal.solutions.palm_flow.modules.empresas.mapper.EmpresaMapper;
import mx.uumbal.solutions.palm_flow.modules.empresas.repository.EmpresaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;

    @Transactional(readOnly = true)
    public Page<EmpresaResponseDTO> getAll(Pageable pageable) {
        return empresaRepository.findAll(pageable).map(empresaMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO getById(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Empresa", id));
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO getBySlug(String slug) {
        Empresa empresa = empresaRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Empresa con slug '" + slug + "' no encontrada"));
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO create(EmpresaRequestDTO dto) {
        if (empresaRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("El slug '" + dto.getSlug() + "' ya está en uso");
        }
        if (empresaRepository.existsByNombre(dto.getNombre())) {
            throw new BadRequestException("Ya existe una empresa con el nombre '" + dto.getNombre() + "'");
        }
        Empresa empresa = empresaMapper.toEntity(dto);
        empresa = empresaRepository.save(empresa);
        log.info("Empresa creada: {} (slug: {})", empresa.getNombre(), empresa.getSlug());
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Empresa", id));
        empresa.setNombre(dto.getNombre());
        empresa.setActivo(dto.isActivo());
        empresa = empresaRepository.save(empresa);
        log.info("Empresa actualizada: {} (id: {})", empresa.getNombre(), empresa.getId());
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional
    public void toggleActivo(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Empresa", id));
        empresa.setActivo(!empresa.isActivo());
        empresaRepository.save(empresa);
        log.info("Empresa {} {}", empresa.getNombre(), empresa.isActivo() ? "activada" : "desactivada");
    }

    public boolean existsBySlug(String slug) {
        if (slug == null) {
            return false;
        }
        return empresaRepository.existsBySlug(slug.trim());
    }

    public boolean isActivo(String slug) {
        if (slug == null) {
            return false;
        }
        return empresaRepository.findBySlug(slug.trim())
                .map(Empresa::isActivo)
                .orElse(false);
    }
}
