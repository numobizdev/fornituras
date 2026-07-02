using System.Security.Claims;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Formats.Jpeg;
using SixLabors.ImageSharp.Metadata.Profiles.Exif;
using SixLabors.ImageSharp.PixelFormats;

namespace Fornituras.Api.Tests;

/// <summary>Utilidades compartidas por los tests de fotos (017): imágenes de prueba y wiring.</summary>
internal static class MediaSupport
{
    static MediaSupport()
    {
        if (!PiiCipher.IsConfigured)
        {
            PiiCipher.Configure(System.Text.Encoding.UTF8.GetBytes("0123456789abcdef0123456789abcdef"));
        }
    }

    public static void EnsureCipher()
    {
        // Fuerza la ejecución del constructor estático.
    }

    public static byte[] PngBytes(int width = 8, int height = 8)
    {
        using var image = new Image<Rgba32>(width, height, new Rgba32(10, 20, 30));
        using var stream = new MemoryStream();
        image.SaveAsPng(stream);
        return stream.ToArray();
    }

    public static byte[] JpegWithExif(int width = 8, int height = 8)
    {
        using var image = new Image<Rgba32>(width, height, new Rgba32(40, 50, 60));
        var exif = new ExifProfile();
        exif.SetValue(ExifTag.Copyright, "clasificado");
        exif.SetValue(ExifTag.GPSLatitudeRef, "N");
        image.Metadata.ExifProfile = exif;
        using var stream = new MemoryStream();
        image.SaveAsJpeg(stream);
        return stream.ToArray();
    }

    public static AppOptions Options(string storagePath, bool officerPhotoEnabled = false) => new()
    {
        Media = new MediaOptions
        {
            StoragePath = storagePath,
            MaxSizeBytes = 5 * 1024 * 1024,
            MaxWidth = 4096,
            MaxHeight = 4096,
            OfficerPhotoEnabled = officerPhotoEnabled
        }
    };

    public static ApplicationDbContext NewDb() =>
        new(new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase($"media-{Guid.NewGuid():N}")
            .Options);

    public static CurrentUserService CurrentUser(ApplicationDbContext db, string? email, params string[] roles)
    {
        var claims = new List<Claim>();
        if (email is not null)
        {
            claims.Add(new Claim(ClaimTypes.Email, email));
        }

        claims.AddRange(roles.Select(r => new Claim(ClaimTypes.Role, r)));

        var accessor = new HttpContextAccessor
        {
            HttpContext = new DefaultHttpContext
            {
                User = new ClaimsPrincipal(new ClaimsIdentity(claims, "test"))
            }
        };

        return new CurrentUserService(accessor, db);
    }

    public static MediaService Service(
        ApplicationDbContext db, string storagePath, CurrentUserService currentUser, bool officerPhotoEnabled = false)
    {
        var options = Microsoft.Extensions.Options.Options.Create(Options(storagePath, officerPhotoEnabled));
        var sanitizer = new ImageSanitizer(options);
        var storage = new LocalEncryptedFileStorage(options);
        return new MediaService(db, sanitizer, storage, currentUser, new NoOpAuditWriter(), options);
    }
}

/// <summary>Auditoría no-op para aislar los tests de servicio.</summary>
internal sealed class NoOpAuditWriter : IAuditWriter
{
    public List<string> Actions { get; } = [];

    public Task RecordAsync(string action, long resourceId, CancellationToken cancellationToken = default)
    {
        Actions.Add(action);
        return Task.CompletedTask;
    }

    public Task RecordEventAsync(string action, string? detail, CancellationToken cancellationToken = default)
    {
        Actions.Add(action);
        return Task.CompletedTask;
    }
}
