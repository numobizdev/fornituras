package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;
import org.springframework.stereotype.Component;

@Component
public class RecepcionFrutaMapperImpl implements RecepcionFrutaMapper {

    @Override
    public RecepcionFrutaResponseDTO toResponseDTO(RecepcionFruta entity) {
        if (entity == null) {
            return null;
        }
        return RecepcionFrutaResponseDTO.builder()
                .uuid(entity.getUuid())
                .folio(entity.getFolio())
                .fecha(entity.getFecha())
                .centroAcopioUuid(entity.getCentroAcopio().getUuid())
                .centroAcopioNombre(entity.getCentroAcopio().getNombre())
                .productorUuid(entity.getProductor().getUuid())
                .productorNombre(entity.getProductor().getNombre())
                .predioUuid(entity.getPredio().getUuid())
                .predioNombre(entity.getPredio().getNombre())
                .loteUuid(entity.getLote() != null ? entity.getLote().getUuid() : null)
                .loteNombre(entity.getLote() != null ? entity.getLote().getNombre() : null)
                .placa(entity.getPlaca())
                .modelo(entity.getModelo())
                .marca(entity.getMarca())
                .tipoColor(entity.getTipoColor())
                .propietario(entity.getPropietario())
                .pesoBruto(entity.getPesoBruto())
                .pesoTara(entity.getPesoTara())
                .pesoNeto(entity.getPesoNeto())
                .precioKg(entity.getPrecioKg())
                .montoAPagar(entity.getMontoAPagar())
                .origenPeso(entity.getOrigenPeso())
                .calidadFruta(entity.getCalidadFruta())
                .usuarioId(entity.getUsuario().getId())
                .usuarioEmail(entity.getUsuario().getEmail())
                .registroOffline(entity.isRegistroOffline())
                .fechaSync(entity.getFechaSync())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public void applyRequest(RecepcionFruta entity, RecepcionFrutaRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setFecha(dto.getFecha());
        entity.setPlaca(dto.getPlaca());
        entity.setModelo(dto.getModelo());
        entity.setMarca(dto.getMarca());
        entity.setTipoColor(dto.getTipoColor());
        entity.setPropietario(dto.getPropietario());
        entity.setPesoBruto(dto.getPesoBruto());
        entity.setPesoTara(dto.getPesoTara());
        entity.setPesoNeto(computePesoNeto(dto.getPesoBruto(), dto.getPesoTara()));
        entity.setOrigenPeso(dto.getOrigenPeso());
        entity.setCalidadFruta(
                dto.getCalidadFruta() != null ? dto.getCalidadFruta() : java.util.List.of());
        entity.setRegistroOffline(Boolean.TRUE.equals(dto.getRegistroOffline()));
        entity.setFechaSync(dto.getFechaSync());
    }

    @Override
    public void applyPricing(RecepcionFruta entity, BigDecimal precioKg) {
        if (entity == null) {
            return;
        }
        entity.setPrecioKg(precioKg);
        entity.setMontoAPagar(computeMontoAPagar(precioKg, entity.getPesoNeto()));
    }

    private BigDecimal computeMontoAPagar(BigDecimal precioKg, BigDecimal pesoNeto) {
        if (precioKg == null || pesoNeto == null) {
            return null;
        }
        return precioKg.multiply(pesoNeto).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computePesoNeto(BigDecimal pesoBruto, BigDecimal pesoTara) {
        if (pesoBruto == null || pesoTara == null) {
            return null;
        }
        BigDecimal pesoNeto = pesoBruto.subtract(pesoTara);
        if (pesoNeto.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El peso tara no puede ser mayor que el peso bruto");
        }
        return pesoNeto;
    }
}
