package mx.uumbal.solutions.palm_flow.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long id;
    private String email;
    private boolean enabled;
    private Set<String> roles;
    private Set<UUID> centroAcopioUuids;
    private Instant createdAt;
    private Instant updatedAt;
}
