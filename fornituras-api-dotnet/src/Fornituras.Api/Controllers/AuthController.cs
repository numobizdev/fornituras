using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public sealed class AuthController(IAuthService authService) : ControllerBase
{
    [AllowAnonymous]
    [HttpPost("login")]
    public async Task<ActionResult<ApiResponse<AuthResponse>>> Login(
        [FromBody] LoginRequest request,
        CancellationToken cancellationToken)
    {
        var response = await authService.LoginAsync(request, cancellationToken);
        return Ok(ApiResponse<AuthResponse>.Ok(response, "Login successful"));
    }

    [AllowAnonymous]
    [HttpPost("activate")]
    public async Task<ActionResult<ApiResponse<object?>>> Activate(
        [FromBody] ActivateAccountRequest request,
        CancellationToken cancellationToken)
    {
        await authService.ActivateAccountAsync(request, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Cuenta activada correctamente. Ya puede iniciar sesión."));
    }

    [Authorize]
    [HttpPost("change-password")]
    public async Task<ActionResult<ApiResponse<object?>>> ChangePassword(
        [FromBody] ChangePasswordRequest request,
        CancellationToken cancellationToken)
    {
        await authService.ChangePasswordAsync(request, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Password changed successfully"));
    }

    [AllowAnonymous]
    [HttpPost("forgot-password")]
    public async Task<ActionResult<ApiResponse<object?>>> ForgotPassword(
        [FromBody] ForgotPasswordRequest request,
        CancellationToken cancellationToken)
    {
        await authService.ForgotPasswordAsync(request, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Si el correo existe, se ha enviado un código de recuperación."));
    }

    [AllowAnonymous]
    [HttpPost("reset-password")]
    public async Task<ActionResult<ApiResponse<object?>>> ResetPassword(
        [FromBody] ResetPasswordRequest request,
        CancellationToken cancellationToken)
    {
        await authService.ResetPasswordAsync(request, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Contraseña restablecida exitosamente. Ahora puede iniciar sesión."));
    }
}
