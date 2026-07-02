using Fornituras.Api.Dto;
using QRCoder;
using SixLabors.Fonts;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Drawing.Processing;
using SixLabors.ImageSharp.Formats.Png;
using SixLabors.ImageSharp.PixelFormats;
using SixLabors.ImageSharp.Processing;

namespace Fornituras.Api.Services;

/// <summary>
/// Generación de imágenes QR con tamaño físico (cm), padding, borde y etiqueta.
/// </summary>
/// <remarks>
/// <paramref name="squareSizeCm"/> es el tamaño total del cuadrado (área de corte/borde).
/// <paramref name="paddingCm"/> es el margen interno blanco por cada lado dentro del cuadrado.
/// El módulo QR mide <c>squareSizeCm - 2 × paddingCm</c> (p. ej. 4.8 cm − 2×0.6 cm = 3.6 cm).
/// </remarks>
internal static class QrImageHelper
{
    private const int Dpi = 300;
    private const double CmToPixels = Dpi / 2.54;
    private const double LabelHeightCm = 0.5;
    private const float LabelFontPt = 8f;
    private const double MinQrModuleCm = 0.5;

    /// <summary>PNG cuadrado: sticker (QR + padding interno + borde opcional), sin etiqueta.</summary>
    public static byte[] GenerateStickerPng(
        string content,
        double squareSizeCm,
        double paddingCm,
        bool drawBorder)
    {
        using var image = GenerateStickerImage(content, squareSizeCm, paddingCm, drawBorder);
        return EncodePng(image);
    }

    /// <summary>PNG completo para ZIP: sticker cuadrado + etiqueta arriba/abajo/ninguna.</summary>
    public static byte[] GenerateCodeUnitPng(
        string content,
        double squareSizeCm,
        double paddingCm,
        LabelPosition labelPosition,
        bool drawBorder)
    {
        using var image = GenerateCodeUnitImage(content, squareSizeCm, paddingCm, labelPosition, drawBorder);
        return EncodePng(image);
    }

    private static Image<Rgba32> GenerateStickerImage(
        string content,
        double squareSizeCm,
        double paddingCm,
        bool drawBorder)
    {
        var qrModuleCm = Math.Max(MinQrModuleCm, squareSizeCm - (2 * paddingCm));
        var squarePx = CmToPx(squareSizeCm);
        var qrPx = CmToPx(qrModuleCm);
        var paddingPx = (squarePx - qrPx) / 2;

        using var qr = LoadQrImage(content, qrPx);
        var canvas = new Image<Rgba32>(squarePx, squarePx);
        canvas.Mutate(ctx =>
        {
            ctx.BackgroundColor(Color.White);
            ctx.DrawImage(qr, new Point(paddingPx, paddingPx), 1f);

            if (drawBorder)
            {
                var borderPx = Math.Max(1f, 0.5f / 72f * Dpi);
                var inset = borderPx / 2f;
                var rect = new RectangleF(
                    inset,
                    inset,
                    squarePx - borderPx,
                    squarePx - borderPx);
                ctx.Draw(Color.Black, borderPx, rect);
            }
        });

        return canvas;
    }

    private static Image<Rgba32> GenerateCodeUnitImage(
        string content,
        double squareSizeCm,
        double paddingCm,
        LabelPosition labelPosition,
        bool drawBorder)
    {
        using var sticker = GenerateStickerImage(content, squareSizeCm, paddingCm, drawBorder);
        var squarePx = sticker.Width;
        var labelHeightPx = labelPosition == LabelPosition.NONE ? 0 : CmToPx(LabelHeightCm);
        var totalHeight = squarePx + labelHeightPx;

        var canvas = new Image<Rgba32>(squarePx, totalHeight);
        canvas.Mutate(ctx =>
        {
            ctx.BackgroundColor(Color.White);

            var stickerY = labelPosition == LabelPosition.TOP ? labelHeightPx : 0;
            ctx.DrawImage(sticker, new Point(0, stickerY), 1f);

            if (labelPosition != LabelPosition.NONE)
            {
                var labelY = labelPosition == LabelPosition.TOP ? 0 : squarePx;
                DrawCenteredLabel(ctx, content, squarePx, labelY, labelHeightPx);
            }
        });

        return canvas;
    }

    private static Image<Rgba32> LoadQrImage(string content, int qrPx)
    {
        using var generator = new QRCodeGenerator();
        using var data = generator.CreateQrCode(content, QRCodeGenerator.ECCLevel.M);
        var png = new PngByteQRCode(data);
        // Sin quiet zone ISO: el padding configurado ya define el margen blanco (paridad Java MARGIN=0).
        var raw = png.GetGraphic(1, drawQuietZones: false);

        using var decoded = Image.Load<Rgba32>(raw);
        return decoded.Clone(ctx => ctx.Resize(qrPx, qrPx, KnownResamplers.NearestNeighbor));
    }

    private static void DrawCenteredLabel(IImageProcessingContext ctx, string text, int width, int y, int height)
    {
        var fontSizePx = (int)Math.Round(LabelFontPt * Dpi / 72.0);
        var font = SystemFonts.CreateFont("Arial", fontSizePx, FontStyle.Regular);
        var options = new RichTextOptions(font)
        {
            HorizontalAlignment = HorizontalAlignment.Center,
            VerticalAlignment = VerticalAlignment.Center,
            Origin = new PointF(width / 2f, y + height / 2f)
        };
        ctx.DrawText(options, text, Color.Black);
    }

    private static int CmToPx(double cm) => (int)Math.Round(cm * CmToPixels);

    private static byte[] EncodePng(Image image)
    {
        using var stream = new MemoryStream();
        image.Save(stream, new PngEncoder());
        return stream.ToArray();
    }
}
