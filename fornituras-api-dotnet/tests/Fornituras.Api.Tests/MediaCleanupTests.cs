using Fornituras.Api.Data.Entities;
using Fornituras.Api.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Tests;

// Cobertura de G-4 (spec 017 FR-016): la limpieza borra la foto huérfana y conserva la referenciada.
public class MediaCleanupTests
{
    public MediaCleanupTests() => MediaSupport.EnsureCipher();

    [Fact]
    public async Task Purges_orphan_but_keeps_referenced_photo()
    {
        var storage = Path.Combine(Path.GetTempPath(), "media-cleanup", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(storage);
        await using var db = MediaSupport.NewDb();
        var options = Options.Create(MediaSupport.Options(storage));
        options.Value.Media.OrphanGraceHours = 1;

        var fileStorage = new LocalEncryptedFileStorage(options);
        var old = DateTime.UtcNow.AddHours(-2);

        var orphan = new MediaAsset { StorageKey = "orphan.enc", ContentType = "image/png", Context = MediaContext.EQUIPMENT, CreatedAt = old, UpdatedAt = old };
        var referenced = new MediaAsset { StorageKey = "referenced.enc", ContentType = "image/png", Context = MediaContext.EQUIPMENT, CreatedAt = old, UpdatedAt = old };
        await fileStorage.StoreAsync(orphan.StorageKey, MediaSupport.PngBytes());
        await fileStorage.StoreAsync(referenced.StorageKey, MediaSupport.PngBytes());
        db.MediaAssets.AddRange(orphan, referenced);

        db.Equipment.Add(new Equipment
        {
            CodigoQr = "FOR-000001",
            CodigoNormalizado = "FOR000001",
            EquipmentTypeId = 1,
            WarehouseId = 1,
            FotoUrl = $"/api/v1/media/{referenced.Id}",
            CreatedAt = old,
            UpdatedAt = old
        });
        await db.SaveChangesAsync();

        var cleanup = new MediaCleanupService(
            db, fileStorage, new NoOpAuditWriter(), options, NullLogger<MediaCleanupService>.Instance);

        var purged = await cleanup.PurgeOrphansAsync();

        Assert.Equal(1, purged);
        Assert.False(await db.MediaAssets.AnyAsync(m => m.Id == orphan.Id));
        Assert.True(await db.MediaAssets.AnyAsync(m => m.Id == referenced.Id));
        Assert.False(File.Exists(Path.Combine(storage, "orphan.enc")));
        Assert.True(File.Exists(Path.Combine(storage, "referenced.enc")));
    }
}
