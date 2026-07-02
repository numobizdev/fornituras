using Fornituras.Api.Data.Entities;
using Fornituras.Api.Services;
using QuestPDF.Infrastructure;
using EntityLabelPosition = Fornituras.Api.Data.Entities.LabelPosition;
using DtoLabelPosition = Fornituras.Api.Dto.LabelPosition;

namespace Fornituras.Api.Tests;

public sealed class QrPdfServiceTests
{
    static QrPdfServiceTests()
    {
        QuestPDF.Settings.License = LicenseType.Community;
    }

    [Fact]
    public void GeneratePdf_WithLargeSquareAndLabel_DoesNotThrow()
    {
        var service = new QrPdfService();
        var lote = new LoteQr
        {
            Id = 1,
            Descripcion = "Lote inicial",
            Cantidad = 10,
            QrSizeCm = 4.8m,
            PaddingCm = 0.6m,
            LabelPosition = EntityLabelPosition.BOTTOM,
            MostrarBordes = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var codigos = Enumerable.Range(1, 10).Select(i => $"FOR-{i:D6}").ToList();

        var pdf = service.GeneratePdf(
            lote,
            codigos,
            4.8m,
            0.6m,
            DtoLabelPosition.BOTTOM,
            true);

        Assert.NotNull(pdf);
        Assert.NotEmpty(pdf);
    }
}
