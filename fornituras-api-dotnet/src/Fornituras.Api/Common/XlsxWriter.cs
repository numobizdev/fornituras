using ClosedXML.Excel;

namespace Fornituras.Api.Common;

/// <summary>
/// Genera libros `.xlsx` reales para las exportaciones de reportes (remedia G-1 / spec 011 FR-003;
/// reemplaza el CSV mal etiquetado por Excel auténtico). Ver ADR 0018 (ClosedXML, MIT).
/// </summary>
public static class XlsxWriter
{
    public static byte[] Build<T>(
        string sheetName,
        string[] headers,
        IReadOnlyList<T> rows,
        Func<T, int, string?[]> cells)
    {
        using var workbook = new XLWorkbook();
        var sheet = workbook.Worksheets.Add(SanitizeSheetName(sheetName));

        for (var c = 0; c < headers.Length; c++)
        {
            sheet.Cell(1, c + 1).Value = headers[c];
        }

        sheet.Row(1).Style.Font.Bold = true;

        for (var i = 0; i < rows.Count; i++)
        {
            var data = cells(rows[i], i);
            for (var c = 0; c < data.Length; c++)
            {
                sheet.Cell(i + 2, c + 1).Value = data[c] ?? string.Empty;
            }
        }

        sheet.Columns().AdjustToContents();

        using var stream = new MemoryStream();
        workbook.SaveAs(stream);
        return stream.ToArray();
    }

    // Excel: nombre de hoja ≤ 31 chars y sin \ / ? * [ ] :
    private static string SanitizeSheetName(string name)
    {
        var cleaned = new string(name.Select(ch => "\\/?*[]:".Contains(ch) ? '-' : ch).ToArray());
        return cleaned.Length > 31 ? cleaned[..31] : cleaned;
    }
}
