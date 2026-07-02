using Fornituras.Api.Common;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;

namespace Fornituras.Api.Services;

/// <summary>
/// Fachada QR que compone generación de lotes, PDF y ZIP.
/// </summary>
public sealed class QrService(
    LoteQrService loteQrService,
    QrPdfService qrPdfService,
    QrZipService qrZipService) : IQrService
{
    public Task<LoteQrResponse> GenerateLoteAsync(GenerateQrForm form, CancellationToken cancellationToken = default) =>
        loteQrService.GenerateAsync(form, cancellationToken);

    public Task<IReadOnlyList<LoteQrResponse>> FindLotesAsync(CancellationToken cancellationToken = default) =>
        loteQrService.FindAllAsync(cancellationToken);

    public Task<LoteQrResponse> FindLoteByIdAsync(long id, CancellationToken cancellationToken = default) =>
        loteQrService.FindByIdAsync(id, cancellationToken);

    public async Task<IReadOnlyList<CodigoQrResponse>> FindCodigosAsync(
        long loteId,
        CancellationToken cancellationToken = default)
    {
        var lote = await loteQrService.GetEntityAsync(loteId, cancellationToken);
        return loteQrService.ListCodigos(lote)
            .Select(c => new CodigoQrResponse(c, loteId))
            .ToList();
    }

    public async Task<byte[]> ExportPdfAsync(long loteId, CancellationToken cancellationToken = default)
    {
        var lote = await loteQrService.GetEntityAsync(loteId, cancellationToken);
        var codigos = loteQrService.ListCodigos(lote);
        return qrPdfService.GeneratePdf(lote, codigos);
    }

    public async Task<byte[]> ExportZipAsync(long loteId, CancellationToken cancellationToken = default)
    {
        var lote = await loteQrService.GetEntityAsync(loteId, cancellationToken);
        var codigos = loteQrService.ListCodigos(lote);
        return qrZipService.GenerateZip(lote, codigos);
    }

    public async Task<byte[]> ReprintPdfAsync(
        long loteId,
        ReprintQrForm form,
        CancellationToken cancellationToken = default)
    {
        var lote = await loteQrService.GetEntityAsync(loteId, cancellationToken);
        var codigos = loteQrService.ListCodigos(lote);
        return qrPdfService.GeneratePdf(lote, codigos, form.QrSizeCm, form.PaddingCm, form.MostrarBordes);
    }

    public async Task<byte[]> ReprintZipAsync(
        long loteId,
        ReprintQrForm form,
        CancellationToken cancellationToken = default)
    {
        var lote = await loteQrService.GetEntityAsync(loteId, cancellationToken);
        var codigos = loteQrService.ListCodigos(lote);
        return qrZipService.GenerateZip(lote, codigos, form.QrSizeCm, form.PaddingCm, form.MostrarBordes);
    }
}
