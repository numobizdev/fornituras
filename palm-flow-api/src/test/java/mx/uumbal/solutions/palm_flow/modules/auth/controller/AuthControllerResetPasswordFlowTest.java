package mx.uumbal.solutions.palm_flow.modules.auth.controller;

import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ForgotPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ResetPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.entity.PasswordResetToken;
import mx.uumbal.solutions.palm_flow.modules.auth.repository.PasswordResetTokenRepository;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerResetPasswordFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String HECTOR_EMAIL = "hector.ramirez@numobiz.com";
    private static final String HECTOR_PASSWORD = "Test1234";
    private static final String NEW_PASSWORD = "NuevaPass2025!";
    private static final String TENANT = "uumbal";

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create();
        if (userRepository.findByEmailAndTenantId(HECTOR_EMAIL, TENANT).isEmpty()) {
            Role userRole = roleRepository.findByName(Role.RoleName.ROLE_ADMINISTRADOR)
                    .orElseThrow(() -> new IllegalStateException("Role not found"));
            User hector = User.builder()
                    .email(HECTOR_EMAIL)
                    .passwordHash(passwordEncoder.encode(HECTOR_PASSWORD))
                    .enabled(true)
                    .roles(java.util.Set.of(userRole))
                    .build();
            userRepository.save(hector);
        }
    }

    private String uri(String path) {
        return "http://localhost:" + port + "/palmFlowApi/api/v1/auth" + path;
    }

    @Test
    @DisplayName("Flujo completo: forgot password -> reset con código -> login con nueva contraseña")
    void fullResetPasswordFlow_withHectorEmail_succeeds() {
        // 1. Solicitar recuperación de contraseña
        var forgotResponse = client.post()
                .uri(uri("/forgot-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ForgotPasswordRequest.builder().email(HECTOR_EMAIL).build())
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(forgotResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(forgotResponse.getBody()).isNotNull();
        assertThat(forgotResponse.getBody().isSuccess()).isTrue();

        // 2. Obtener el código de verificación desde la BD
        User hector = userRepository.findByEmailAndTenantId(HECTOR_EMAIL, TENANT)
                .orElseThrow(() -> new IllegalStateException("Hector user not found"));

        PasswordResetToken resetToken = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(hector.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Reset token not created"));

        String code = resetToken.getCode();
        assertThat(code).isNotNull();
        assertThat(code).matches("\\d{6}");

        // 3. Restablecer contraseña con el código
        var resetResponse = client.post()
                .uri(uri("/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResetPasswordRequest.builder()
                        .code(code)
                        .newPassword(NEW_PASSWORD)
                        .build())
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(resetResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resetResponse.getBody()).isNotNull();
        assertThat(resetResponse.getBody().isSuccess()).isTrue();

        // 4. Verificar que se puede iniciar sesión con la nueva contraseña
        var loginResponse = client.post()
                .uri(uri("/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(LoginRequest.builder()
                        .email(HECTOR_EMAIL)
                        .password(NEW_PASSWORD)
                        .tenantId(TENANT)
                        .build())
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().isSuccess()).isTrue();
        assertThat(loginResponse.getBody().getData()).isNotNull();
    }

    @Test
    @DisplayName("Login con contraseña anterior falla después del reseteo")
    void oldPasswordFails_afterReset() {
        // 1. Solicitar recuperación
        client.post()
                .uri(uri("/forgot-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ForgotPasswordRequest.builder().email(HECTOR_EMAIL).build())
                .retrieve()
                .toEntity(ApiResponse.class);

        // 2. Obtener código
        User hector = userRepository.findByEmailAndTenantId(HECTOR_EMAIL, TENANT)
                .orElseThrow();
        PasswordResetToken resetToken = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(hector.getId()))
                .findFirst()
                .orElseThrow();

        // 3. Resetear con nueva contraseña
        client.post()
                .uri(uri("/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResetPasswordRequest.builder()
                        .code(resetToken.getCode())
                        .newPassword(NEW_PASSWORD)
                        .build())
                .retrieve()
                .toEntity(ApiResponse.class);

        // 4. Login con la contraseña antigua debe fallar
        var oldLoginResponse = client.post()
                .uri(uri("/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(LoginRequest.builder()
                        .email(HECTOR_EMAIL)
                        .password(HECTOR_PASSWORD)
                        .tenantId(TENANT)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .toEntity(ApiResponse.class);

        assertThat(oldLoginResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("El código de verificación es de un solo uso")
    void resetCode_isSingleUse() {
        // 1. Solicitar recuperación
        client.post()
                .uri(uri("/forgot-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ForgotPasswordRequest.builder().email(HECTOR_EMAIL).build())
                .retrieve()
                .toEntity(ApiResponse.class);

        // 2. Obtener código
        User hector = userRepository.findByEmailAndTenantId(HECTOR_EMAIL, TENANT)
                .orElseThrow();
        PasswordResetToken resetToken = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(hector.getId()))
                .findFirst()
                .orElseThrow();

        String code = resetToken.getCode();

        // 3. Primer uso (debe funcionar)
        var firstUse = client.post()
                .uri(uri("/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResetPasswordRequest.builder()
                        .code(code)
                        .newPassword(NEW_PASSWORD)
                        .build())
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(firstUse.getStatusCode().is2xxSuccessful()).isTrue();

        // 4. Segundo uso con el mismo código (debe fallar)
        var secondUse = client.post()
                .uri(uri("/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResetPasswordRequest.builder()
                        .code(code)
                        .newPassword("OtraPass2025!")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .toEntity(ApiResponse.class);

        assertThat(secondUse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("Código inválido devuelve error")
    void invalidCode_returnsError() {
        var response = client.post()
                .uri(uri("/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResetPasswordRequest.builder()
                        .code("999999")
                        .newPassword(NEW_PASSWORD)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
