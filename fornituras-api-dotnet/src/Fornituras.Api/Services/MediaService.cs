using Fornituras.Api.Common;
using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Services;

/// <summary>
/// Orquesta la subida y lectura de fotos (017): valida peso, sanea (formato/dimensiones/EXIF),
/// aplica RBAC + gating para PII, cifra y persiste; la lectura descifra con control de acceso.
/// </summary>
public interface IMediaService
{
    Task<MediaUploadResponse> UploadAsync(
        byte[] content, string contextRaw, CancellationToken cancellationToken = default);

    Task<(byte[] Bytes, string ContentType)> LoadAsync(Guid id, CancellationToken cancellationToken = default);

    Task DeleteAsync(Guid id, CancellationToken cancellationToken = default);
}

public sealed class MediaService(
    ApplicationDbContext db,
    ImageSanitizer sanitizer,
    IFileStorage storage,
    CurrentUserService currentUser,
    IAuditWriter audit,
    IOptions<AppOptions> options) : IMediaService
{
    private readonly MediaOptions _media = options.Value.Media;

    public async Task<MediaUploadResponse> UploadAsync(
        byte[] content, string contextRaw, CancellationToken cancellationToken = default)
    {
        var context = ParseContext(contextRaw);
        var isPii = context == MediaContext.OFFICER;

        if (isPii)
        {
            // Gate legal (ADR 0003): la foto de elemento no se habilita sin base legal confirmada.
            if (!_media.OfficerPhotoEnabled)
            {
                throw new ForbiddenException("La captura de foto de elemento está deshabilitada.");
            }

            if (!RolePolicy.CanCaptureOfficerPhoto(currentUser.User))
            {
                throw new ForbiddenException("No tienes autorización para subir la foto de un elemento.");
            }
        }

        if (content.LongLength > _media.MaxSizeBytes)
        {
            throw new PayloadTooLargeException(
                $"La imagen supera el tamaño máximo permitido ({_media.MaxSizeBytes} bytes).");
        }

        var sanitized = sanitizer.Sanitize(content);

        var now = DateTime.UtcNow;
        var asset = new MediaAsset
        {
            Id = Guid.NewGuid(),
            StorageKey = BuildStorageKey(now),
            ContentType = sanitized.ContentType,
            SizeBytes = sanitized.Bytes.LongLength,
            IsPii = isPii,
            Context = context,
            CreatedAt = now,
            UpdatedAt = now
        };

        await storage.StoreAsync(asset.StorageKey, sanitized.Bytes, cancellationToken);
        db.MediaAssets.Add(asset);
        await db.SaveChangesAsync(cancellationToken);

        if (isPii)
        {
            await audit.RecordEventAsync("UPLOAD_OFFICER_PHOTO", asset.Id.ToString(), cancellationToken);
        }

        return new MediaUploadResponse(asset.Id, $"/api/v1/media/{asset.Id}", asset.ContentType);
    }

    public async Task<(byte[] Bytes, string ContentType)> LoadAsync(
        Guid id, CancellationToken cancellationToken = default)
    {
        var asset = await db.MediaAssets.AsNoTracking()
            .FirstOrDefaultAsync(a => a.Id == id, cancellationToken)
            ?? throw new NotFoundException("Foto no encontrada.");

        if (asset.IsPii && !RolePolicy.CanViewOfficerPhoto(currentUser.User))
        {
            // 403 => el cliente muestra la foto enmascarada (enmascaramiento por defecto de PII).
            throw new ForbiddenException("No tienes autorización para ver esta foto.");
        }

        var bytes = await storage.LoadAsync(asset.StorageKey, cancellationToken);

        if (asset.IsPii)
        {
            await audit.RecordEventAsync("VIEW_OFFICER_PHOTO", asset.Id.ToString(), cancellationToken);
        }

        return (bytes, asset.ContentType);
    }

    public async Task DeleteAsync(Guid id, CancellationToken cancellationToken = default)
    {
        var asset = await db.MediaAssets.FirstOrDefaultAsync(a => a.Id == id, cancellationToken)
            ?? throw new NotFoundException("Foto no encontrada.");

        if (asset.IsPii && !RolePolicy.CanCaptureOfficerPhoto(currentUser.User))
        {
            throw new ForbiddenException("No tienes autorización para eliminar esta foto.");
        }

        await storage.DeleteAsync(asset.StorageKey, cancellationToken);
        db.MediaAssets.Remove(asset);
        await db.SaveChangesAsync(cancellationToken);

        if (asset.IsPii)
        {
            await audit.RecordEventAsync("DELETE_OFFICER_PHOTO", asset.Id.ToString(), cancellationToken);
        }
    }

    private static MediaContext ParseContext(string? contextRaw)
    {
        if (string.IsNullOrWhiteSpace(contextRaw) ||
            !Enum.TryParse<MediaContext>(contextRaw.Trim(), ignoreCase: true, out var context))
        {
            throw new BadRequestException("Contexto de foto inválido.");
        }

        return context;
    }

    private static string BuildStorageKey(DateTime now) =>
        $"{now:yyyy}/{now:MM}/{Guid.NewGuid():N}.enc";
}
