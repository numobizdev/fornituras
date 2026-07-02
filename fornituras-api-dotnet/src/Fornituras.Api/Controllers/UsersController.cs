using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/users")]
public sealed class UsersController(IUserService userService) : ControllerBase
{
    [HttpGet("{id:long}")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> GetById(long id, CancellationToken cancellationToken)
    {
        if (!User.IsInRole("ADMIN") && !await userService.IsCurrentUserAsync(id, cancellationToken))
        {
            return Forbid();
        }

        var user = await userService.FindByIdAsync(id, cancellationToken);
        return Ok(ApiResponse<UserResponse>.Ok(user));
    }

    [Authorize(Roles = RolePolicy.ManageUsers)]
    [HttpGet]
    public async Task<ActionResult<ApiResponse<PageResult<UserResponse>>>> GetAll(
        [FromQuery] PaginationQuery pagination,
        [FromQuery] string? role,
        [FromQuery] bool? enabled,
        CancellationToken cancellationToken)
    {
        var page = await userService.FindAllAsync(pagination, role, enabled, cancellationToken);
        return Ok(ApiResponse<PageResult<UserResponse>>.Ok(page));
    }

    [Authorize(Roles = RolePolicy.ManageUsers)]
    [HttpPost]
    public async Task<ActionResult<ApiResponse<UserResponse>>> Create(
        [FromBody] UserRequest request,
        CancellationToken cancellationToken)
    {
        var user = await userService.CreateAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created,
            ApiResponse<UserResponse>.Ok(user, "Usuario creado. Se envió un código de activación al correo."));
    }

    [Authorize(Roles = RolePolicy.ManageUsers)]
    [HttpPut("{id:long}")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> Update(
        long id,
        [FromBody] UserUpdateRequest request,
        CancellationToken cancellationToken)
    {
        var user = await userService.UpdateAsync(id, request, cancellationToken);
        return Ok(ApiResponse<UserResponse>.Ok(user, "Usuario actualizado."));
    }

    [Authorize(Roles = RolePolicy.ManageUsers)]
    [HttpPatch("{id:long}/enabled")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> SetEnabled(
        long id,
        [FromBody] EnabledUpdateRequest request,
        CancellationToken cancellationToken)
    {
        var user = await userService.SetEnabledAsync(id, request.Enabled, cancellationToken);
        var message = request.Enabled ? "Usuario activado." : "Usuario desactivado.";
        return Ok(ApiResponse<UserResponse>.Ok(user, message));
    }

    [Authorize(Roles = RolePolicy.ManageUsers)]
    [HttpPatch("{id:long}/role")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> ChangeRole(
        long id,
        [FromBody] RoleUpdateRequest request,
        CancellationToken cancellationToken)
    {
        var user = await userService.ChangeRoleAsync(id, request.Role, cancellationToken);
        return Ok(ApiResponse<UserResponse>.Ok(user, "Rol actualizado."));
    }
}
