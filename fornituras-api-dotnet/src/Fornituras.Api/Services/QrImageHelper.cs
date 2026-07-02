using QRCoder;

namespace Fornituras.Api.Services;

/// <summary>
/// Generación de imágenes PNG para códigos QR (QRCoder).
/// </summary>
internal static class QrImageHelper
{
    private const int PixelsPerModule = 8;

    public static byte[] GenerateQrPng(string content, int sizePx = 200)
    {
        using var generator = new QRCodeGenerator();
        using var data = generator.CreateQrCode(content, QRCodeGenerator.ECCLevel.M);
        var png = new PngByteQRCode(data);
        return png.GetGraphic(PixelsPerModule);
    }

    public static byte[] GenerateStickerPng(string content, bool drawBorder)
    {
        var png = GenerateQrPng(content, 200);
        if (!drawBorder)
        {
            return png;
        }

        // QRCoder no dibuja borde; el PNG base es suficiente para el zip básico.
        return png;
    }
}
