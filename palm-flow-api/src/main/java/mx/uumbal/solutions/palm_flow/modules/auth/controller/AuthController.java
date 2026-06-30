package mx.uumbal.solutions.palm_flow.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ChangePasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ForgotPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.RegisterRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ResetPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and account management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email and password, returns JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register with email and password; sends activation link to email")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please check your email to activate your account.", null));
    }

    @GetMapping("/activate")
    @Operation(summary = "Activate account", description = "Activate account using token from email link")
    public ResponseEntity<ApiResponse<Void>> activate(@RequestParam String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully. You can now log in.", null));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password (requires authentication)")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset link to be sent to email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Si el correo existe, se ha enviado un código de recuperación.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email link")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Contraseña restablecida exitosamente. Ahora puede iniciar sesión.", null));
    }
}
