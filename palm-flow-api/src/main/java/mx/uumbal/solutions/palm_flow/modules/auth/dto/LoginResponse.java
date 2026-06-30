package mx.uumbal.solutions.palm_flow.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String email;
    private Long userId;
    private List<String> roles;
    private String tenantId;
    private List<UUID> centroAcopioUuids;
}
