using Fornituras.Api.Services;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.PixelFormats;

namespace Fornituras.Api.Tests;

public sealed class QrImageHelperTests
{
    private const int Dpi = 300;
    private const double CmToPixels = Dpi / 2.54;

    [Fact]
    public void GenerateStickerPng_QrPatternFillsAreaMinusPadding()
    {
        const double squareCm = 4.8;
        const double paddingCm = 0.6;

        var png = QrImageHelper.GenerateStickerPng("FOR-000005", squareCm, paddingCm, drawBorder: false);
        using var image = Image.Load<Rgba32>(png);

        var squarePx = (int)Math.Round(squareCm * CmToPixels);
        var paddingPx = (int)Math.Round(paddingCm * CmToPixels);
        var expectedPatternPx = squarePx - (2 * paddingPx);

        Assert.Equal(squarePx, image.Width);
        Assert.Equal(squarePx, image.Height);

        var bounds = GetNonWhiteBounds(image);
        var patternWidth = bounds.MaxX - bounds.MinX + 1;
        var patternHeight = bounds.MaxY - bounds.MinY + 1;

        // Tolerancia ±2 px por redondeo al convertir cm→px.
        Assert.InRange(patternWidth, expectedPatternPx - 2, expectedPatternPx + 2);
        Assert.InRange(patternHeight, expectedPatternPx - 2, expectedPatternPx + 2);
        Assert.InRange(bounds.MinX, paddingPx - 2, paddingPx + 2);
        Assert.InRange(bounds.MinY, paddingPx - 2, paddingPx + 2);
    }

    private static (int MinX, int MinY, int MaxX, int MaxY) GetNonWhiteBounds(Image<Rgba32> image)
    {
        var minX = image.Width;
        var minY = image.Height;
        var maxX = 0;
        var maxY = 0;

        for (var y = 0; y < image.Height; y++)
        {
            for (var x = 0; x < image.Width; x++)
            {
                if (image[x, y].R < 250)
                {
                    minX = Math.Min(minX, x);
                    minY = Math.Min(minY, y);
                    maxX = Math.Max(maxX, x);
                    maxY = Math.Max(maxY, y);
                }
            }
        }

        return (minX, minY, maxX, maxY);
    }
}
