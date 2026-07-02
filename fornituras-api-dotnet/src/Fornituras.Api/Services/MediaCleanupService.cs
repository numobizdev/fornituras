using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Services;

/// <summary>
/// Limpieza de fotos huérfanas (017 FR-016): purga los `media_asset` **sin asociar** a ningún
/// equipo/tipo/elemento tras un periodo de gracia. Una foto es huérfana si su id no aparece en el
/// `foto_url` de ninguna entidad. Borrado destructivo de PII → gobernado por `OrphanCleanupEnabled`.
/// </summary>
public sealed class MediaCleanupService(
    ApplicationDbContext db,
    IFileStorage storage,
    IAuditWriter audit,
    IOptions<AppOptions> options,
    ILogger<MediaCleanupService> logger)
{
    private readonly MediaOptions _media = options.Value.Media;

    /// <summary>Purga las fotos huérfanas más antiguas que el periodo de gracia. Devuelve cuántas borró.</summary>
    public async Task<int> PurgeOrphansAsync(CancellationToken cancellationToken = default)
    {
        var threshold = DateTime.UtcNow.AddHours(-_media.OrphanGraceHours);
        var candidates = await db.MediaAssets
            .Where(m => m.CreatedAt < threshold)
            .ToListAsync(cancellationToken);

        if (candidates.Count == 0)
        {
            return 0;
        }

        var referenced = await CollectReferencedIdsAsync(cancellationToken);

        var purged = 0;
        foreach (var asset in candidates.Where(a => !referenced.Contains(a.Id)))
        {
            await storage.DeleteAsync(asset.StorageKey, cancellationToken);
            db.MediaAssets.Remove(asset);
            if (asset.IsPii)
            {
                await audit.RecordEventAsync("PURGE_ORPHAN_OFFICER_PHOTO", asset.Id.ToString(), cancellationToken);
            }

            purged++;
        }

        if (purged > 0)
        {
            await db.SaveChangesAsync(cancellationToken);
            logger.LogInformation("Purged {Count} orphan media assets.", purged);
        }

        return purged;
    }

    private async Task<HashSet<Guid>> CollectReferencedIdsAsync(CancellationToken cancellationToken)
    {
        var urls = new List<string>();
        urls.AddRange(await db.Officers.Where(o => o.FotoUrl != null)
            .Select(o => o.FotoUrl!).ToListAsync(cancellationToken));
        urls.AddRange(await db.Equipment.Where(e => e.FotoUrl != null)
            .Select(e => e.FotoUrl!).ToListAsync(cancellationToken));
        urls.AddRange(await db.CatalogItems.Where(i => i.FotoUrl != null)
            .Select(i => i.FotoUrl!).ToListAsync(cancellationToken));

        return urls.Select(ExtractMediaId)
            .Where(id => id.HasValue)
            .Select(id => id!.Value)
            .ToHashSet();
    }

    /// <summary>Extrae el GUID de una referencia interna (`/api/v1/media/&lt;guid&gt;` o el guid desnudo).</summary>
    private static Guid? ExtractMediaId(string reference)
    {
        var trimmed = reference.Trim();
        var lastSegment = trimmed.Split('/', StringSplitOptions.RemoveEmptyEntries).LastOrDefault();
        return Guid.TryParse(lastSegment, out var id) ? id : null;
    }
}
