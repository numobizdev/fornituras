using Fornituras.Api.Common;
using Fornituras.Api.Data.Entities;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;

namespace Fornituras.Api.Services;

public sealed class QrPdfService
{
    static QrPdfService()
    {
        QuestPDF.Settings.License = LicenseType.Community;
    }

    public byte[] GeneratePdf(LoteQr lote, IReadOnlyList<string> codigos) =>
        GeneratePdf(lote, codigos, lote.QrSizeCm, lote.PaddingCm, lote.MostrarBordes);

    public byte[] GeneratePdf(
        LoteQr lote,
        IReadOnlyList<string> codigos,
        decimal qrSizeCm,
        decimal paddingCm,
        bool mostrarBordes)
    {
        if (codigos.Count == 0)
        {
            throw new BadRequestException("No QR codes provided for PDF generation");
        }

        const int cols = 4;
        const float qrSizePt = 72f;

        return Document.Create(container =>
        {
            container.Page(page =>
            {
                page.Size(PageSizes.A4);
                page.Margin(1, Unit.Centimetre);
                page.DefaultTextStyle(x => x.FontSize(8));

                page.Header().Column(col =>
                {
                    col.Item().Text($"Lote QR #{lote.Id}").Bold().FontSize(12);
                    col.Item().Text(lote.Descripcion);
                    col.Item().Text($"Generado: {DateTime.UtcNow:dd/MM/yyyy HH:mm} UTC");
                });

                page.Content().PaddingTop(10).Grid(grid =>
                {
                    grid.Columns(cols);
                    grid.Spacing(5);

                    foreach (var codigo in codigos)
                    {
                        var png = QrImageHelper.GenerateStickerPng(codigo, mostrarBordes);
                        grid.Item().Border(0.5f).Padding(4).Column(item =>
                        {
                            item.Item().Height(qrSizePt).Image(png).FitArea();
                            item.Item().AlignCenter().Text(codigo).FontSize(7);
                        });
                    }
                });
            });
        }).GeneratePdf();
    }
}
