using System.IO.Compression;
using Fornituras.Api.Common;
using Fornituras.Api.Data.Entities;
using DtoLabelPosition = Fornituras.Api.Dto.LabelPosition;
using EntityLabelPosition = Fornituras.Api.Data.Entities.LabelPosition;

namespace Fornituras.Api.Services;

public sealed class QrZipService
{
    public byte[] GenerateZip(LoteQr lote, IReadOnlyList<string> codigos) =>
        GenerateZip(
            lote,
            codigos,
            lote.QrSizeCm,
            lote.PaddingCm,
            ToDtoLabel(lote.LabelPosition),
            lote.MostrarBordes);

    public byte[] GenerateZip(
        LoteQr lote,
        IReadOnlyList<string> codigos,
        decimal qrSizeCm,
        decimal paddingCm,
        DtoLabelPosition labelPosition,
        bool mostrarBordes)
    {
        if (codigos.Count == 0)
        {
            throw new BadRequestException("No QR codes provided for ZIP generation");
        }

        using var memoryStream = new MemoryStream();
        using (var archive = new ZipArchive(memoryStream, ZipArchiveMode.Create, leaveOpen: true))
        {
            var usedNames = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (var codigo in codigos)
            {
                var fileName = SanitizeFileName(codigo, usedNames);
                var entry = archive.CreateEntry(fileName, CompressionLevel.Fastest);
                using var entryStream = entry.Open();
                var png = QrImageHelper.GenerateCodeUnitPng(
                    codigo,
                    (double)qrSizeCm,
                    (double)paddingCm,
                    labelPosition,
                    mostrarBordes);
                entryStream.Write(png);
            }
        }

        return memoryStream.ToArray();
    }

    private static string SanitizeFileName(string codigo, HashSet<string> usedNames)
    {
        var safe = string.Concat(codigo.Select(c =>
            char.IsLetterOrDigit(c) || c is '-' or '_' ? c : '_'));
        var fileName = $"{safe}.png";
        var candidate = fileName;
        var suffix = 1;
        while (!usedNames.Add(candidate))
        {
            candidate = $"{safe}_{suffix++}.png";
        }

        return candidate;
    }

    private static DtoLabelPosition ToDtoLabel(EntityLabelPosition position) =>
        Enum.Parse<DtoLabelPosition>(position.ToString());
}
