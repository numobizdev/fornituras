using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

public sealed class UserService(
    ApplicationDbContext db,
    AuthService authService,
    CurrentUserService currentUser,
    IAuditWriter audit,
    ILogger<UserService> logger) : IUserService
{
    public async Task<UserResponse> FindByIdAsync(long id, CancellationToken cancellationToken = default) =>
        ToResponse(await GetOrThrowAsync(id, cancellationToken));

    public async Task<PageResult<UserResponse>> FindAllAsync(
        PaginationQuery pagination,
        string? role = null,
        bool? enabled = null,
        CancellationToken cancellationToken = default)
    {
        var query = db.Users.AsNoTracking().AsQueryable();

        if (!string.IsNullOrWhiteSpace(role))
        {
            if (!Enum.TryParse<Role>(role, true, out var parsedRole))
            {
                throw new BadRequestException($"Rol inválido: {role}");
            }

            query = query.Where(u => u.Role == parsedRole);
        }

        if (enabled is not null)
        {
            query = query.Where(u => u.Enabled == enabled);
        }

        var ordered = query.OrderBy(u => u.Id);
        var total = await ordered.CountAsync(cancellationToken);
        var items = await ordered
            .Skip(pagination.Page * pagination.Size)
            .Take(pagination.Size)
            .ToListAsync(cancellationToken);
        return PageResult<UserResponse>.From(
            items.Select(ToResponse).ToList(), total, pagination.Page, pagination.Size);
    }

    public async Task<UserResponse> FindByEmailAsync(string email, CancellationToken cancellationToken = default)
    {
        var user = await db.Users.AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == email, cancellationToken)
            ?? throw new NotFoundException($"User not found with email: {email}");
        return ToResponse(user);
    }

    public async Task<bool> IsCurrentUserAsync(long id, CancellationToken cancellationToken = default)
    {
        var email = currentUser.Email;
        if (string.IsNullOrWhiteSpace(email))
        {
            return false;
        }

        var user = await db.Users.AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == email, cancellationToken);
        return user?.Id == id;
    }

    public async Task<UserResponse> CreateAsync(UserRequest request, CancellationToken cancellationToken = default)
    {
        var email = request.Email.Trim();
        logger.LogInformation("Creating user with email: {Email}", email);

        if (await db.Users.AnyAsync(u => u.Email == email, cancellationToken))
        {
            throw new ConflictException($"Email already registered: {email}");
        }

        var role = Enum.TryParse<Role>(request.Role, true, out var parsedRole)
            ? parsedRole
            : Role.CAPTURISTA;

        var now = DateTime.UtcNow;
        var user = new User
        {
            Name = request.Name.Trim(),
            Email = email,
            Role = role,
            Enabled = false,
            Password = BCrypt.Net.BCrypt.HashPassword(Guid.NewGuid().ToString("N") + Guid.NewGuid().ToString("N")),
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Users.Add(user);
        await db.SaveChangesAsync(cancellationToken);
        await authService.SendActivationCodeAsync(user, cancellationToken);
        await audit.RecordAsync("CREATE_USER", user.Id, cancellationToken);
        logger.LogInformation("Pending user created with id: {UserId}", user.Id);
        return ToResponse(user);
    }

    public async Task<UserResponse> UpdateAsync(
        long id,
        UserUpdateRequest request,
        CancellationToken cancellationToken = default)
    {
        var user = await GetOrThrowAsync(id, cancellationToken);
        user.Name = request.Name.Trim();
        user.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("UPDATE_USER", user.Id, cancellationToken);
        return ToResponse(user);
    }

    public async Task<UserResponse> SetEnabledAsync(
        long id,
        bool enabled,
        CancellationToken cancellationToken = default)
    {
        var user = await GetOrThrowAsync(id, cancellationToken);
        if (WouldLeaveSystemWithoutAdmin(user, user.Role == Role.ADMIN, enabled))
        {
            throw new ConflictException("No se puede desactivar al último administrador activo del sistema.");
        }

        user.Enabled = enabled;
        user.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync(enabled ? "ENABLE_USER" : "DISABLE_USER", user.Id, cancellationToken);
        return ToResponse(user);
    }

    public async Task<UserResponse> ChangeRoleAsync(
        long id,
        string role,
        CancellationToken cancellationToken = default)
    {
        if (!Enum.TryParse<Role>(role, true, out var newRole))
        {
            throw new BadRequestException($"Rol inválido: {role}");
        }

        return await ChangeRoleInternalAsync(id, newRole, cancellationToken);
    }

    private async Task<UserResponse> ChangeRoleInternalAsync(
        long id,
        Role newRole,
        CancellationToken cancellationToken = default)
    {
        var user = await GetOrThrowAsync(id, cancellationToken);
        if (WouldLeaveSystemWithoutAdmin(user, newRole == Role.ADMIN, user.Enabled))
        {
            throw new ConflictException("No se puede quitar el rol de administrador al último admin activo.");
        }

        user.Role = newRole;
        user.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("ROLE_CHANGE_USER", user.Id, cancellationToken);
        return ToResponse(user);
    }

    private bool WouldLeaveSystemWithoutAdmin(User user, bool willBeAdmin, bool willBeEnabled)
    {
        var isCurrentlyActiveAdmin = user.Role == Role.ADMIN && user.Enabled;
        if (!isCurrentlyActiveAdmin)
        {
            return false;
        }

        var staysActiveAdmin = willBeAdmin && willBeEnabled;
        if (staysActiveAdmin)
        {
            return false;
        }

        return db.Users.Count(u => u.Role == Role.ADMIN && u.Enabled) <= 1;
    }

    private async Task<User> GetOrThrowAsync(long id, CancellationToken cancellationToken)
    {
        return await db.Users.FirstOrDefaultAsync(u => u.Id == id, cancellationToken)
            ?? throw new NotFoundException($"User not found with id: {id}");
    }

    private static UserResponse ToResponse(User user) =>
        new(user.Id, user.Name, user.Email, user.Role.ToString(), user.Enabled, user.CreatedAt, user.UpdatedAt);
}
