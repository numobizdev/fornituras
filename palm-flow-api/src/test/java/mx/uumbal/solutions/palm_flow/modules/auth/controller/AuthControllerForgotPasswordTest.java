package mx.uumbal.solutions.palm_flow.modules.auth.controller;

import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ForgotPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.repository.PasswordResetTokenRepository;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class AuthControllerForgotPasswordTest {

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
    private static final String TENANT = "uumbal";

    @BeforeEach
    void setUpHectorUser() {
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

    @Test
    void forgotPassword_withExistingEmail_returnsSuccessAndCreatesToken() {
        long initialTokenCount = tokenRepository.count();

        var request = ForgotPasswordRequest.builder()
                .email("contacto@numobiz.net")
                .build();

        var response = RestClient.create()
                .post()
                .uri("http://localhost:" + port + "/palmFlowApi/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        long tokenCountAfter = tokenRepository.count();
        assertThat(tokenCountAfter).isEqualTo(initialTokenCount + 1);
    }

    @Test
    void forgotPassword_withNonExistentEmail_returnsSuccessWithoutToken() {
        long initialTokenCount = tokenRepository.count();

        var request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        var response = RestClient.create()
                .post()
                .uri("http://localhost:" + port + "/palmFlowApi/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        long tokenCountAfter = tokenRepository.count();
        assertThat(tokenCountAfter).isEqualTo(initialTokenCount);
    }

    @Test
    void forgotPassword_withRegisteredHectorRamirezEmail_returnsSuccessAndCreatesToken() {
        long initialTokenCount = tokenRepository.count();

        var request = ForgotPasswordRequest.builder()
                .email(HECTOR_EMAIL)
                .build();

        var response = RestClient.create()
                .post()
                .uri("http://localhost:" + port + "/palmFlowApi/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        long tokenCountAfter = tokenRepository.count();
        assertThat(tokenCountAfter).isEqualTo(initialTokenCount + 1);
    }

    @Test
    void forgotPassword_withInvalidEmail_returnsBadRequest() {
        var request = ForgotPasswordRequest.builder()
                .email("not-an-email")
                .build();

        var response = RestClient.create()
                .post()
                .uri("http://localhost:" + port + "/palmFlowApi/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
