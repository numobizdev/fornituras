package mx.uumbal.solutions.palm_flow.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ExcelSeedPathResolver {

    private ExcelSeedPathResolver() {
    }

    static Path resolve(String configuredPath) {
        List<Path> candidates = new ArrayList<>();
        Path configured = Path.of(configuredPath);
        candidates.add(configured);
        if (!configured.isAbsolute()) {
            String userDir = System.getProperty("user.dir");
            candidates.add(Path.of(userDir).resolve(configuredPath));
            candidates.add(Path.of(userDir, "palm-flow-api").resolve(configuredPath));
        }
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        return configured.toAbsolutePath().normalize();
    }
}
