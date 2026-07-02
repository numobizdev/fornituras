using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

/// <summary>
/// Protección anti-fuerza-bruta: bloqueo tras 5 intentos fallidos por 15 minutos (FR-005).
/// </summary>
public sealed class LoginAttemptService(ApplicationDbContext db, IAuditWriter audit)
{
    private const int MaxAttempts = 5;
    private const int LockMinutes = 15;

    public void AssertNotLocked(User user)
    {
        if (user.LockedUntil is not null && user.LockedUntil > DateTime.UtcNow)
        {
            throw new TooManyRequestsException(
                "Cuenta temporalmente bloqueada por varios intentos fallidos. Intente más tarde.");
        }
    }

    public async Task OnFailedAttemptAsync(long userId, CancellationToken cancellationToken = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);
        if (user is null)
        {
            return;
        }

        var attempts = user.FailedAttempts + 1;
        user.FailedAttempts = attempts;
        if (attempts >= MaxAttempts)
        {
            user.LockedUntil = DateTime.UtcNow.AddMinutes(LockMinutes);
            await audit.RecordAsync("LOGIN_LOCKED", user.Id, cancellationToken);
        }

        user.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
    }

    public async Task OnSuccessfulLoginAsync(long userId, CancellationToken cancellationToken = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);
        if (user is null)
        {
            return;
        }

        if (user.FailedAttempts != 0 || user.LockedUntil is not null)
        {
            user.FailedAttempts = 0;
            user.LockedUntil = null;
            user.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(cancellationToken);
        }
    }
}
