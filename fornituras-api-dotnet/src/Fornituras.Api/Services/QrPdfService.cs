using Fornituras.Api.Common;
using Fornituras.Api.Data.Entities;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;
using DtoLabelPosition = Fornituras.Api.Dto.LabelPosition;
using EntityLabelPosition = Fornituras.Api.Data.Entities.LabelPosition;

namespace Fornituras.Api.Services;

public sealed class QrPdfService
{
    private const float CmToPointsFactor = 72f / 2.54f;
    private const float PageMarginCm = 1.0f;
    private const float LabelHeightCm = 0.5f;
    private const float A4WidthCm = 21.0f;
    private const float A4HeightCm = 29.7f;
    private const float MinHorizontalGapPt = 5f;
    private const float HeaderReserveCm = 4.5f;

    static QrPdfService()
    {
        QuestPDF.Settings.License = LicenseType.Community;
    }

    public byte[] GeneratePdf(LoteQr lote, IReadOnlyList<string> codigos) =>
        GeneratePdf(
            lote,
            codigos,
            lote.QrSizeCm,
            lote.PaddingCm,
            ToDtoLabel(lote.LabelPosition),
            lote.MostrarBordes);

    public byte[] GeneratePdf(
        LoteQr lote,
        IReadOnlyList<string> codigos,
        decimal qrSizeCm,
        decimal paddingCm,
        DtoLabelPosition labelPosition,
        bool mostrarBordes)
    {
        if (codigos.Count == 0)
        {
            throw new BadRequestException("No QR codes provided for PDF generation");
        }

        var squareSize = (float)qrSizeCm;
        var padding = (float)paddingCm;
        var layout = ComputePageLayout(squareSize, labelPosition);
        var rowsFirstPage = Math.Max(
            1,
            (int)Math.Floor((layout.UsableHeightPt - CmToPoints(HeaderReserveCm)) / layout.CellHeightPt) - 1);

        var pages = new List<(int StartIndex, int Count, bool ShowHeader)>();
        var index = 0;
        var firstPage = true;
        while (index < codigos.Count)
        {
            var rowsThisPage = firstPage ? rowsFirstPage : layout.RowsPerPage;
            var capacity = layout.Cols * rowsThisPage;
            var count = Math.Min(capacity, codigos.Count - index);
            pages.Add((index, count, firstPage));
            index += count;
            firstPage = false;
        }

        return Document.Create(container =>
        {
            foreach (var (startIndex, count, showHeader) in pages)
            {
                container.Page(page =>
                {
                    page.Size(PageSizes.A4);
                    page.Margin(CmToPoints(PageMarginCm));
                    page.DefaultTextStyle(x => x.FontSize(8));

                    page.Content().Column(col =>
                    {
                        if (showHeader)
                        {
                            col.Item().Element(c => BuildHeader(c, lote, layout));
                            col.Item().PaddingTop(8);
                        }

                        col.Item().Element(c =>
                            BuildCodesGrid(
                                c,
                                codigos,
                                startIndex,
                                count,
                                layout,
                                labelPosition,
                                squareSize,
                                padding,
                                mostrarBordes));
                    });
                });
            }
        }).GeneratePdf();
    }

    private static void BuildHeader(IContainer container, LoteQr lote, PageLayout layout)
    {
        container.Column(col =>
        {
            col.Item().Text($"Lote QR #{lote.Id}").Bold().FontSize(12);
            col.Item().Text($"Descripción: {lote.Descripcion}");
            col.Item().Text($"Fecha: {lote.CreatedAt:dd/MM/yyyy HH:mm}");
            col.Item().Text($"Cantidad de códigos: {lote.Cantidad}");
            col.Item().Text(
                $"Disposición: {layout.Cols} por fila × {layout.RowsPerPage} filas por página " +
                $"(máx. {layout.CapacityPerFullPage} códigos/página)");
        });
    }

    private static void BuildCodesGrid(
        IContainer container,
        IReadOnlyList<string> codigos,
        int startIndex,
        int count,
        PageLayout layout,
        DtoLabelPosition labelPosition,
        float squareSizeCm,
        float paddingCm,
        bool mostrarBordes)
    {
        var squarePt = layout.CellWidthPt;
        var labelHeightPt = labelPosition == DtoLabelPosition.NONE ? 0f : CmToPoints(LabelHeightCm);
        var rows = (int)Math.Ceiling(count / (double)layout.Cols);

        container.AlignLeft().Table(table =>
        {
            table.ColumnsDefinition(columns =>
            {
                for (var col = 0; col < layout.Cols; col++)
                {
                    columns.ConstantColumn(squarePt);
                    if (col < layout.Cols - 1)
                    {
                        columns.RelativeColumn();
                    }
                }
            });

            for (var row = 0; row < rows; row++)
            {
                for (var col = 0; col < layout.Cols; col++)
                {
                    var cellIndex = row * layout.Cols + col;
                    if (cellIndex < count)
                    {
                        var codigo = codigos[startIndex + cellIndex];
                        table.Cell().Element(cell =>
                            BuildCodeUnit(
                                cell,
                                codigo,
                                squarePt,
                                labelHeightPt,
                                labelPosition,
                                squareSizeCm,
                                paddingCm,
                                mostrarBordes));
                    }
                    else
                    {
                        table.Cell();
                    }

                    if (col < layout.Cols - 1)
                    {
                        table.Cell();
                    }
                }
            }
        });
    }

    private static void BuildCodeUnit(
        IContainer container,
        string codigo,
        float squarePt,
        float labelHeightPt,
        DtoLabelPosition labelPosition,
        float squareSizeCm,
        float paddingCm,
        bool mostrarBordes)
    {
        var stickerPng = QrImageHelper.GenerateStickerPng(codigo, squareSizeCm, paddingCm, mostrarBordes);

        container.Column(col =>
        {
            if (labelPosition == DtoLabelPosition.TOP && labelHeightPt > 0)
            {
                col.Item()
                    .Height(labelHeightPt)
                    .AlignCenter()
                    .AlignMiddle()
                    .Text(codigo)
                    .FontSize(8)
                    .ClampLines(1);
            }

            col.Item()
                .Width(squarePt)
                .Height(squarePt)
                .Image(stickerPng);

            if (labelPosition == DtoLabelPosition.BOTTOM && labelHeightPt > 0)
            {
                col.Item()
                    .Height(labelHeightPt)
                    .AlignCenter()
                    .AlignMiddle()
                    .Text(codigo)
                    .FontSize(8)
                    .ClampLines(1);
            }
        });
    }

    private static PageLayout ComputePageLayout(float squareSizeCm, DtoLabelPosition labelPosition)
    {
        var labelHeightCm = labelPosition == DtoLabelPosition.NONE ? 0f : LabelHeightCm;
        var cellWidthCm = squareSizeCm;
        var cellHeightCm = squareSizeCm + labelHeightCm;

        var usableWidthPt = CmToPoints(A4WidthCm - (2 * PageMarginCm));
        var usableHeightPt = CmToPoints(A4HeightCm - (2 * PageMarginCm));
        var cellWidthPt = CmToPoints(cellWidthCm);
        var cellHeightPt = CmToPoints(cellHeightCm);

        var cols = CalculateMaxColumns(usableWidthPt, cellWidthPt);
        var horizontalGapPt = CalculateHorizontalGapPt(cols, usableWidthPt, cellWidthPt);
        var rowsPerPage = Math.Max(1, (int)Math.Floor(usableHeightPt / cellHeightPt));

        return new PageLayout(cols, rowsPerPage, cellWidthPt, cellHeightPt, horizontalGapPt, usableWidthPt, usableHeightPt);
    }

    private static int CalculateMaxColumns(float usableWidthPt, float cellWidthPt)
    {
        if (cellWidthPt > usableWidthPt)
        {
            return 1;
        }

        return Math.Max(1, (int)Math.Floor((usableWidthPt + MinHorizontalGapPt) / (cellWidthPt + MinHorizontalGapPt)));
    }

    private static float CalculateHorizontalGapPt(int cols, float usableWidthPt, float cellWidthPt)
    {
        if (cols <= 1)
        {
            return 0f;
        }

        return Math.Max(0f, (usableWidthPt - (cols * cellWidthPt)) / (cols - 1));
    }

    private static float CmToPoints(float cm) => cm * CmToPointsFactor;

    private static DtoLabelPosition ToDtoLabel(EntityLabelPosition position) =>
        Enum.Parse<DtoLabelPosition>(position.ToString());

    private sealed record PageLayout(
        int Cols,
        int RowsPerPage,
        float CellWidthPt,
        float CellHeightPt,
        float HorizontalGapPt,
        float UsableWidthPt,
        float UsableHeightPt)
    {
        public int CapacityPerFullPage => Cols * RowsPerPage;
    }
}
