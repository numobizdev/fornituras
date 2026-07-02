using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

public sealed class AuthService(
    ApplicationDbContext db,
    JwtTokenService jwtTokenService,
    EmailService emailService,
    IAuditWriter audit,
    LoginAttemptService loginAttempt,
    CurrentUserService currentUser,
    ILogger<AuthService> logger) : IAuthService
{
    private const int ActivationCodeHours = 24;
    private const int ResetCodeHours = 1;

    public async Task<AuthResponse> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default)
    {
        var email = request.Email.Trim();
        logger.LogInformation("Login attempt for email: {Email}", email);

        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == email, cancellationToken);
        if (user is null)
        {
            // Auditar el intento denegado (FR-006, spec 012) sin registrar el correo (Principio V).
            await audit.RecordEventAsync("LOGIN_FAILED", "reason=unknown-user", cancellationToken);
            throw new UnauthorizedAppException("Invalid email or password");
        }

        loginAttempt.AssertNotLocked(user);

        if (!user.Enabled)
        {
            await audit.RecordAsync("LOGIN_DENIED_DISABLED", user.Id, cancellationToken);
            throw new UnauthorizedAppException(
                "La cuenta no está activada. Revise su correo para el código de activación.");
        }

        if (!BCrypt.Net.BCrypt.Verify(request.Password, user.Password))
        {
            logger.LogWarning("Login failed: password mismatch for user id {UserId}", user.Id);
            await loginAttempt.OnFailedAttemptAsync(user.Id, cancellationToken);
            await audit.RecordAsync("LOGIN_FAILED", user.Id, cancellationToken);
            throw new UnauthorizedAppException("Invalid email or password");
        }

        await loginAttempt.OnSuccessfulLoginAsync(user.Id, cancellationToken);

        var token = jwtTokenService.GenerateToken(user.Email, user.Role.ToString());
        logger.LogInformation("User logged in: {Email}", user.Email);
        await audit.RecordAsync("LOGIN", user.Id, cancellationToken);
        return BuildAuthResponse(token, user);
    }

    public async Task SendActivationCodeAsync(User user, CancellationToken cancellationToken = default)
    {
        var existing = await db.VerificationTokens
            .Where(t => t.UserId == user.Id)
            .ToListAsync(cancellationToken);
        db.VerificationTokens.RemoveRange(existing);

        var verificationToken = new VerificationToken
        {
            Code = await GenerateUniqueVerificationCodeAsync(cancellationToken),
            UserId = user.Id,
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(ActivationCodeHours)
        };
        db.VerificationTokens.Add(verificationToken);
        await db.SaveChangesAsync(cancellationToken);

        await emailService.SendHtmlEmailAsync(
            user.Email,
            "Activación de cuenta - Fornituras",
            "email/activate-account",
            new Dictionary<string, object>
            {
                ["code"] = verificationToken.Code,
                ["email"] = user.Email,
                ["name"] = user.Name
            },
            cancellationToken);

        logger.LogInformation("Activation email sent to {Email}", user.Email);
    }

    public async Task ActivateAccountAsync(ActivateAccountRequest request, CancellationToken cancellationToken = default)
    {
        var verificationToken = await db.VerificationTokens
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.Code == request.Code, cancellationToken)
            ?? throw new BadRequestException("Código de activación inválido o expirado");

        if (verificationToken.IsExpired())
        {
            db.VerificationTokens.Remove(verificationToken);
            await db.SaveChangesAsync(cancellationToken);
            throw new BadRequestException("El código de activación ha expirado");
        }

        var user = verificationToken.User;
        user.Password = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.Enabled = true;
        user.UpdatedAt = DateTime.UtcNow;
        db.VerificationTokens.Remove(verificationToken);
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Account activated: {Email}", user.Email);
    }

    public async Task ChangePasswordAsync(ChangePasswordRequest request, CancellationToken cancellationToken = default)
    {
        var email = currentUser.Email
            ?? throw new UnauthorizedAppException("User not found");

        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == email, cancellationToken)
            ?? throw new UnauthorizedAppException("User not found");

        if (!BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.Password))
        {
            throw new BadRequestException("Current password is incorrect");
        }

        user.Password = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Password changed for user: {Email}", email);
    }

    public async Task ForgotPasswordAsync(ForgotPasswordRequest request, CancellationToken cancellationToken = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == request.Email.Trim(), cancellationToken);
        if (user is null || !user.Enabled)
        {
            return;
        }

        var existing = await db.PasswordResetTokens
            .Where(t => t.UserId == user.Id)
            .ToListAsync(cancellationToken);
        db.PasswordResetTokens.RemoveRange(existing);

        var resetToken = new PasswordResetToken
        {
            Code = await GenerateUniqueResetCodeAsync(cancellationToken),
            UserId = user.Id,
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(ResetCodeHours)
        };
        db.PasswordResetTokens.Add(resetToken);
        await db.SaveChangesAsync(cancellationToken);

        await emailService.SendHtmlEmailAsync(
            user.Email,
            "Recuperación de contraseña - Fornituras",
            "email/reset-password",
            new Dictionary<string, object>
            {
                ["code"] = resetToken.Code,
                ["email"] = user.Email
            },
            cancellationToken);

        logger.LogInformation("Reset password email sent to {Email}", user.Email);
    }

    public async Task ResetPasswordAsync(ResetPasswordRequest request, CancellationToken cancellationToken = default)
    {
        var resetToken = await db.PasswordResetTokens
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.Code == request.Code, cancellationToken)
            ?? throw new BadRequestException("Código de verificación inválido o expirado");

        if (resetToken.IsExpired())
        {
            db.PasswordResetTokens.Remove(resetToken);
            await db.SaveChangesAsync(cancellationToken);
            throw new BadRequestException("El código de verificación ha expirado");
        }

        var user = resetToken.User;
        user.Password = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;
        db.PasswordResetTokens.Remove(resetToken);
        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Password reset for user: {Email}", user.Email);
    }

    private AuthResponse BuildAuthResponse(string token, User user) =>
        new(
            token,
            "Bearer",
            jwtTokenService.ExpirationMs,
            new UserSummary(user.Id, user.Name, user.Email, user.Role.ToString()));

    private async Task<string> GenerateUniqueVerificationCodeAsync(CancellationToken cancellationToken) =>
        await GenerateUniqueCodeAsync(
            code => db.VerificationTokens.AnyAsync(t => t.Code == code, cancellationToken),
            cancellationToken);

    private async Task<string> GenerateUniqueResetCodeAsync(CancellationToken cancellationToken) =>
        await GenerateUniqueCodeAsync(
            code => db.PasswordResetTokens.AnyAsync(t => t.Code == code, cancellationToken),
            cancellationToken);

    private static async Task<string> GenerateUniqueCodeAsync(
        Func<string, Task<bool>> exists,
        CancellationToken cancellationToken)
    {
        for (var attempt = 0; attempt < 100; attempt++)
        {
            var code = GenerateSixDigitCode();
            if (!await exists(code))
            {
                return code;
            }
        }

        throw new BadRequestException("Unable to generate verification code. Please try again.");
    }

    private static string GenerateSixDigitCode()
    {
        var code = Random.Shared.Next(100_000, 999_999);
        return code.ToString();
    }
}
