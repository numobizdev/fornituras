package mx.uumbal.solutions.palm_flow.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.common.exception.UnauthorizedException;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ChangePasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ForgotPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.LoginResponse;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.RegisterRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.dto.ResetPasswordRequest;
import mx.uumbal.solutions.palm_flow.modules.auth.entity.PasswordResetToken;
import mx.uumbal.solutions.palm_flow.modules.auth.entity.VerificationToken;
import mx.uumbal.solutions.palm_flow.modules.auth.repository.PasswordResetTokenRepository;
import mx.uumbal.solutions.palm_flow.modules.auth.repository.VerificationTokenRepository;
import mx.uumbal.solutions.palm_flow.config.EmpresaDataLoader;
import mx.uumbal.solutions.palm_flow.modules.empresas.service.EmpresaService;
import mx.uumbal.solutions.palm_flow.modules.users.entity.Role;
import mx.uumbal.solutions.palm_flow.modules.users.entity.User;
import mx.uumbal.solutions.palm_flow.modules.users.repository.RoleRepository;
import mx.uumbal.solutions.palm_flow.modules.users.repository.UserRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import mx.uumbal.solutions.palm_flow.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int VERIFICATION_TOKEN_HOURS = 24;
    private static final int RESET_TOKEN_HOURS = 1;
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmpresaService empresaService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String tenantId = EmpresaDataLoader.slugOrDefault(request.getTenantId());
        String email = request.getEmail().trim();
        TenantContext.set(tenantId);

        if (!empresaService.existsBySlug(tenantId)) {
            throw new UnauthorizedException("Empresa no encontrada");
        }
        if (!empresaService.isActivo(tenantId)) {
            throw new UnauthorizedException("La empresa está inactiva");
        }

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> {
                    log.warn("Login failed: no user for email+tenant slug [{}]", tenantId);
                    return new UnauthorizedException("Invalid email or password");
                });
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is not activated. Please check your email for the activation link.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: password mismatch for user id [{}]", user.getId());
            throw new UnauthorizedException("Invalid email or password");
        }
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());
        List<UUID> centroAcopioUuids = userRepository.findCentroAcopioUuidsByUserId(user.getId());
        String token = jwtService.generateToken(user.getEmail(), roles, tenantId);
        log.info("User logged in: {} (tenant: {})", user.getEmail(), tenantId);
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .roles(roles)
                .tenantId(tenantId)
                .centroAcopioUuids(centroAcopioUuids)
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        String tenantId = EmpresaDataLoader.slugOrDefault(request.getTenantId());
        TenantContext.set(tenantId);

        if (!empresaService.existsBySlug(tenantId)) {
            throw new BadRequestException("Empresa no encontrada");
        }
        if (!empresaService.isActivo(tenantId)) {
            throw new BadRequestException("La empresa está inactiva");
        }
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BadRequestException("Email already registered");
        }
        Role defaultRole = roleRepository.findByName(Role.RoleName.ROLE_PRODUCTOR)
                .orElseThrow(() -> new BadRequestException("Default role not found"));

        // tenantId is automatically set by Hibernate via @TenantId from TenantContext
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .roles(Set.of(defaultRole))
                .build();
        user = userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(VERIFICATION_TOKEN_HOURS * 3600L))
                .createdAt(Instant.now())
                .build();
        verificationTokenRepository.save(verificationToken);

        String activationLink = frontendBaseUrl + "/activate?token=" + tokenValue;
        emailService.sendHtmlEmail(
                user.getEmail(),
                "Activate your Palm Flow account",
                "email/verify-account",
                Map.of("activationLink", activationLink, "email", user.getEmail())
        );
        log.info("Registration email sent to {} (tenant: {})", user.getEmail(), tenantId);
    }

    @Transactional
    public void activateAccount(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired activation link"));
        if (vt.isExpired()) {
            verificationTokenRepository.delete(vt);
            throw new BadRequestException("Activation link has expired");
        }
        User user = vt.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.delete(vt);
        log.info("Account activated: {}", user.getEmail());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = EmpresaDataLoader.slugOrDefault(TenantContext.get());
        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {} (tenant: {})", email, tenantId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String tenantId = EmpresaDataLoader.slugOrDefault(request.getTenantId());
        TenantContext.set(tenantId);

        userRepository.findByEmailAndTenantId(request.getEmail(), tenantId).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());
            passwordResetTokenRepository.flush();
            String tokenValue = UUID.randomUUID().toString();
            String code = generateCode();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .token(tokenValue)
                    .code(code)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(RESET_TOKEN_HOURS * 3600L))
                    .createdAt(Instant.now())
                    .build();
            passwordResetTokenRepository.save(prt);
            emailService.sendHtmlEmail(
                    user.getEmail(),
                    "Recuperación de contraseña - Palm Flow",
                    "email/reset-password",
                    Map.of("code", code, "email", user.getEmail())
            );
            log.info("Reset password email sent to {} with code {}", user.getEmail(), code);
        });
        // Always return success to avoid email enumeration
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = passwordResetTokenRepository.findByCode(request.getCode())
                .orElseThrow(() -> new BadRequestException("Código de verificación inválido o expirado"));
        if (prt.isExpired()) {
            passwordResetTokenRepository.delete(prt);
            throw new BadRequestException("El código de verificación ha expirado");
        }
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(prt);
        log.info("Password reset for user: {}", user.getEmail());
    }

    private String generateCode() {
        int code = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }
}
