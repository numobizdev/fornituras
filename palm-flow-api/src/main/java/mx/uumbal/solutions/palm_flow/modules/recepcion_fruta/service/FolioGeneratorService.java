package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service;

import java.time.Year;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository.RecepcionFrutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FolioGeneratorService {

    private final RecepcionFrutaRepository recepcionFrutaRepository;

    @Transactional
    public synchronized String generateFolio() {
        int year = Year.now().getValue();
        String prefix = "RF" + year;
        int nextSequence = recepcionFrutaRepository
                .findTopByFolioStartingWithOrderByFolioDesc(prefix)
                .map(existing -> {
                    String sequencePart = existing.getFolio().substring(prefix.length());
                    return Integer.parseInt(sequencePart) + 1;
                })
                .orElse(1);
        return prefix + String.format("%06d", nextSequence);
    }
}
