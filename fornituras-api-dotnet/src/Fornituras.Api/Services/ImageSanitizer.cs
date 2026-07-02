using Fornituras.Api.Common;
using Fornituras.Api.Configuration;
using Microsoft.Extensions.Options;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Formats;
using SixLabors.ImageSharp.Formats.Jpeg;
using SixLabors.ImageSharp.Formats.Png;
using SixLabors.ImageSharp.Formats.Webp;

namespace Fornituras.Api.Services;

/// <summary>Imagen saneada: bytes re-codificados sin metadatos, su content-type y dimensiones.</summary>
public sealed record SanitizedImage(byte[] Bytes, string ContentType, int Width, int Height);

/// <summary>
/// Valida y sanea imágenes subidas (017): solo acepta JPEG/PNG/WEBP reales (por firma, no por
/// extensión ni content-type declarado), rechaza SVG/otros, valida dimensiones y **re-codifica
/// eliminando metadatos** (EXIF/IPTC/XMP pueden contener GPS/PII). Ver docs/02-seguridad.md.
/// </summary>
public sealed class ImageSanitizer(IOptions<AppOptions> options)
{
    private readonly MediaOptions _media = options.Value.Media;

    public SanitizedImage Sanitize(byte[] input)
    {
        if (input is null || input.Length == 0)
        {
            throw new BadRequestException("La imagen está vacía.");
        }

        var format = DetectSupportedFormat(input)
            ?? throw new BadRequestException("El archivo no es una imagen válida (JPEG/PNG/WEBP).");

        Image image;
        try
        {
            image = Image.Load(input);
        }
        catch (Exception)
        {
            throw new BadRequestException("El archivo no es una imagen válida (JPEG/PNG/WEBP).");
        }

        using (image)
        {
            if (image.Width > _media.MaxWidth || image.Height > _media.MaxHeight)
            {
                throw new UnprocessableEntityException(
                    $"La imagen excede las dimensiones máximas ({_media.MaxWidth}x{_media.MaxHeight}).");
            }

            // Eliminar metadatos que pueden filtrar PII (ubicación, dispositivo, autor).
            image.Metadata.ExifProfile = null;
            image.Metadata.IptcProfile = null;
            image.Metadata.XmpProfile = null;

            using var output = new MemoryStream();
            var contentType = ReencodeTo(image, format, output);
            return new SanitizedImage(output.ToArray(), contentType, image.Width, image.Height);
        }
    }

    private static IImageFormat? DetectSupportedFormat(byte[] input)
    {
        IImageFormat? format;
        try
        {
            format = Image.DetectFormat(input);
        }
        catch (Exception)
        {
            return null;
        }

        return format is JpegFormat or PngFormat or WebpFormat ? format : null;
    }

    private static string ReencodeTo(Image image, IImageFormat format, Stream output)
    {
        switch (format)
        {
            case JpegFormat:
                image.SaveAsJpeg(output);
                return "image/jpeg";
            case PngFormat:
                image.SaveAsPng(output);
                return "image/png";
            case WebpFormat:
                image.SaveAsWebp(output);
                return "image/webp";
            default:
                throw new BadRequestException("Formato de imagen no soportado.");
        }
    }
}
