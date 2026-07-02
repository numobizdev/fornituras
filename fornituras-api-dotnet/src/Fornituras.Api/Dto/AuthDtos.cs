namespace Fornituras.Api.Dto;

public sealed record LoginRequest(string Email, string Password);

public sealed record AuthResponse(
    string Token,
    string TokenType,
    long ExpiresIn,
    UserSummary User);

public sealed record UserSummary(
    long Id,
    string Name,
    string Email,
    string Role);

public sealed record ActivateAccountRequest(string Code, string NewPassword);

public sealed record ChangePasswordRequest(string CurrentPassword, string NewPassword);

public sealed record ForgotPasswordRequest(string Email);

public sealed record ResetPasswordRequest(string Code, string NewPassword);
