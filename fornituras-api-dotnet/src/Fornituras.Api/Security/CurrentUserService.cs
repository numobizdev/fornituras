using System.Security.Claims;
using Fornituras.Api.Data;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Security;

/// <summary>
/// Resuelve el actor autenticado desde <see cref="HttpContext"/>.
/// </summary>
public sealed class CurrentUserService(IHttpContextAccessor httpContextAccessor, ApplicationDbContext db)
{
    public string? Email =>
        httpContextAccessor.HttpContext?.User?.FindFirstValue(ClaimTypes.Email)
        ?? httpContextAccessor.HttpContext?.User?.FindFirstValue(ClaimTypes.Name)
        ?? httpContextAccessor.HttpContext?.User?.Identity?.Name;

    public async Task<long?> GetUserIdAsync(CancellationToken cancellationToken = default)
    {
        var email = Email;
        if (string.IsNullOrWhiteSpace(email))
        {
            return null;
        }

        return await db.Users
            .AsNoTracking()
            .Where(u => u.Email == email)
            .Select(u => (long?)u.Id)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public ClaimsPrincipal? User => httpContextAccessor.HttpContext?.User;
}
