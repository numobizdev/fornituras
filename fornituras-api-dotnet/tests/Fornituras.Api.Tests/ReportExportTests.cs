using ClosedXML.Excel;
using Fornituras.Api.Common;

namespace Fornituras.Api.Tests;

// Cobertura de G-1 (spec 011 FR-003): la exportación produce un .xlsx real, no CSV.
public class ReportExportTests
{
    [Fact]
    public void Build_produces_a_valid_xlsx_with_header_and_rows()
    {
        string[] headers = ["Codigo", "Descripcion"];
        var rows = new[] { ("FOR-000001", "Chaleco"), ("FOR-000002", "Casco") };

        var bytes = XlsxWriter.Build("Reporte", headers, rows, (row, _) => [row.Item1, row.Item2]);

        // Un .xlsx es un ZIP OOXML: debe empezar con la firma "PK".
        Assert.True(bytes.Length > 2 && bytes[0] == (byte)'P' && bytes[1] == (byte)'K');

        using var workbook = new XLWorkbook(new MemoryStream(bytes));
        var sheet = workbook.Worksheets.First();
        Assert.Equal("Codigo", sheet.Cell(1, 1).GetString());
        Assert.Equal("FOR-000001", sheet.Cell(2, 1).GetString());
        Assert.Equal("Casco", sheet.Cell(3, 2).GetString());
    }
}
