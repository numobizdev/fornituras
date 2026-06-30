package mx.uumbal.solutions.palm_flow.modules.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Usuarios module (placeholder)")
public class UsuarioController {

    @GetMapping("/health")
    @Operation(summary = "Module health", description = "Placeholder endpoint for usuarios module")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("module", "usuarios", "status", "ok")));
    }
}
