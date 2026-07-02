using Fornituras.Api.Data.Entities;

namespace Fornituras.Api.Common;

/// <summary>
/// Deriva el estado de vigencia a partir de la fecha de vencimiento, sin persistirlo.
/// </summary>
public static class ExpiryCalculator
{
    /// <summary>Ventana de aviso previo al vencimiento (FR-007).</summary>
    public const int WarningWindowDays = 90;

    public static ExpiryStatus? StatusFor(DateOnly? expiryDate, DateOnly today)
    {
        if (expiryDate is null)
        {
            return null;
        }

        if (expiryDate.Value < today)
        {
            return ExpiryStatus.CADUCADA;
        }

        if (expiryDate.Value <= today.AddDays(WarningWindowDays))
        {
            return ExpiryStatus.PROXIMA_A_VENCER;
        }

        return ExpiryStatus.VIGENTE;
    }
}
