using System.Text;
using Fornituras.Api.Common;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using SixLabors.ImageSharp;

namespace Fornituras.Api.Tests;

public class ImageSanitizerTests
{
    private static ImageSanitizer NewSanitizer() =>
        new(Options.Create(MediaSupport.Options(Path.GetTempPath())));

    [Fact]
    public void Sanitizes_valid_png()
    {
        var result = NewSanitizer().Sanitize(MediaSupport.PngBytes());
        Assert.Equal("image/png", result.ContentType);
        Assert.True(result.Bytes.Length > 0);
        Assert.Equal(8, result.Width);
    }

    [Fact]
    public void Rejects_svg_disguised_as_image()
    {
        var svg = Encoding.UTF8.GetBytes("<svg xmlns='http://www.w3.org/2000/svg'><script/></svg>");
        Assert.Throws<BadRequestException>(() => NewSanitizer().Sanitize(svg));
    }

    [Fact]
    public void Rejects_non_image_bytes()
    {
        var text = Encoding.UTF8.GetBytes("esto no es una imagen");
        Assert.Throws<BadRequestException>(() => NewSanitizer().Sanitize(text));
    }

    [Fact]
    public void Rejects_oversized_dimensions()
    {
        var options = Options.Create(MediaSupport.Options(Path.GetTempPath()));
        options.Value.Media.MaxWidth = 4;
        options.Value.Media.MaxHeight = 4;
        var sanitizer = new ImageSanitizer(options);
        Assert.Throws<UnprocessableEntityException>(() => sanitizer.Sanitize(MediaSupport.PngBytes(8, 8)));
    }

    [Fact]
    public void Strips_exif_metadata_on_reencode()
    {
        var withExif = MediaSupport.JpegWithExif();
        var sanitized = NewSanitizer().Sanitize(withExif);

        using var reloaded = Image.Load(sanitized.Bytes);
        Assert.Null(reloaded.Metadata.ExifProfile);
    }
}

public class PiiCipherBytesTests
{
    public PiiCipherBytesTests() => MediaSupport.EnsureCipher();

    [Fact]
    public void Round_trips_bytes()
    {
        var plain = MediaSupport.PngBytes();
        var encrypted = PiiCipher.EncryptBytes(plain);

        Assert.NotEqual(plain, encrypted);
        Assert.Equal(plain, PiiCipher.DecryptBytes(encrypted));
    }
}

public class MediaServiceTests
{
    public MediaServiceTests() => MediaSupport.EnsureCipher();

    private static string TempDir()
    {
        var dir = Path.Combine(Path.GetTempPath(), "media-tests", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        return dir;
    }

    [Fact]
    public async Task Uploads_equipment_photo_encrypted_and_loads_it_back()
    {
        var storage = TempDir();
        await using var db = MediaSupport.NewDb();
        var currentUser = MediaSupport.CurrentUser(db, "captura@fornituras.local", "CAPTURISTA");
        var service = MediaSupport.Service(db, storage, currentUser);

        var original = MediaSupport.PngBytes();
        var uploaded = await service.UploadAsync(original, "equipment");

        var asset = await db.MediaAssets.SingleAsync();
        Assert.False(asset.IsPii);
        Assert.Equal(MediaContext.EQUIPMENT, asset.Context);

        // El objeto en disco está cifrado: no coincide con la imagen saneada.
        var onDisk = await File.ReadAllBytesAsync(Path.Combine(storage, asset.StorageKey));
        var (loaded, contentType) = await service.LoadAsync(uploaded.Id);
        Assert.NotEqual(loaded, onDisk);
        Assert.Equal("image/png", contentType);
        Assert.True(loaded.Length > 0);
    }

    [Fact]
    public async Task Rejects_officer_photo_when_gate_disabled()
    {
        var storage = TempDir();
        await using var db = MediaSupport.NewDb();
        var currentUser = MediaSupport.CurrentUser(db, "admin@fornituras.local", "ADMIN");
        var service = MediaSupport.Service(db, storage, currentUser, officerPhotoEnabled: false);

        await Assert.ThrowsAsync<ForbiddenException>(
            () => service.UploadAsync(MediaSupport.PngBytes(), "officer"));
    }

    [Fact]
    public async Task Rejects_officer_photo_for_unauthorized_role_even_when_gate_enabled()
    {
        var storage = TempDir();
        await using var db = MediaSupport.NewDb();
        var currentUser = MediaSupport.CurrentUser(db, "almacen@fornituras.local", "ALMACEN");
        var service = MediaSupport.Service(db, storage, currentUser, officerPhotoEnabled: true);

        await Assert.ThrowsAsync<ForbiddenException>(
            () => service.UploadAsync(MediaSupport.PngBytes(), "officer"));
    }

    [Fact]
    public async Task Masks_pii_photo_from_unauthorized_viewer()
    {
        var storage = TempDir();
        await using var db = MediaSupport.NewDb();

        // Sube como CAPTURISTA (puede capturar) con el gate habilitado.
        var uploader = MediaSupport.CurrentUser(db, "captura@fornituras.local", "CAPTURISTA");
        var uploaded = await MediaSupport.Service(db, storage, uploader, officerPhotoEnabled: true)
            .UploadAsync(MediaSupport.PngBytes(), "officer");

        var asset = await db.MediaAssets.SingleAsync();
        Assert.True(asset.IsPii);

        // ALMACEN no puede ver PII sin enmascarar -> 403 (el cliente muestra estado enmascarado).
        var viewer = MediaSupport.CurrentUser(db, "almacen@fornituras.local", "ALMACEN");
        var viewerService = MediaSupport.Service(db, storage, viewer, officerPhotoEnabled: true);
        await Assert.ThrowsAsync<ForbiddenException>(() => viewerService.LoadAsync(uploaded.Id));

        // SUPERVISOR sí puede.
        var supervisor = MediaSupport.CurrentUser(db, "sup@fornituras.local", "SUPERVISOR");
        var supervisorService = MediaSupport.Service(db, storage, supervisor, officerPhotoEnabled: true);
        var (bytes, _) = await supervisorService.LoadAsync(uploaded.Id);
        Assert.True(bytes.Length > 0);
    }

    [Fact]
    public async Task Rejects_invalid_context()
    {
        var storage = TempDir();
        await using var db = MediaSupport.NewDb();
        var currentUser = MediaSupport.CurrentUser(db, "admin@fornituras.local", "ADMIN");
        var service = MediaSupport.Service(db, storage, currentUser);

        await Assert.ThrowsAsync<BadRequestException>(
            () => service.UploadAsync(MediaSupport.PngBytes(), "bogus"));
    }
}
