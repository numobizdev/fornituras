using Fornituras.Api.Configuration;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Services;

/// <summary>
/// Tarea de fondo que ejecuta periódicamente la limpieza de fotos huérfanas (017 FR-016). Inactiva
/// salvo que `App:Media:OrphanCleanupEnabled` sea verdadero (borrado destructivo de PII, opt-in).
/// </summary>
public sealed class MediaCleanupHostedService(
    IServiceScopeFactory scopeFactory,
    IOptions<AppOptions> options,
    ILogger<MediaCleanupHostedService> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var media = options.Value.Media;
        if (!media.OrphanCleanupEnabled)
        {
            logger.LogInformation("Limpieza de fotos huérfanas deshabilitada (OrphanCleanupEnabled=false).");
            return;
        }

        var interval = TimeSpan.FromHours(Math.Max(1, media.CleanupIntervalHours));

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                using var scope = scopeFactory.CreateScope();
                var cleanup = scope.ServiceProvider.GetRequiredService<MediaCleanupService>();
                await cleanup.PurgeOrphansAsync(stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Fallo en la limpieza de fotos huérfanas.");
            }

            try
            {
                await Task.Delay(interval, stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }
}
