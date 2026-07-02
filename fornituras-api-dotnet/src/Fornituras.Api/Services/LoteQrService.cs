using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;
using EntityLabelPosition = Fornituras.Api.Data.Entities.LabelPosition;
using DtoLabelPosition = Fornituras.Api.Dto.LabelPosition;

namespace Fornituras.Api.Services;

public sealed class LoteQrService(
    ApplicationDbContext db,
    QrCodeGeneratorService qrCodeGenerator,
    IAuditWriter audit)
{
    public async Task<LoteQrResponse> GenerateAsync(
        GenerateQrForm form,
        CancellationToken cancellationToken = default)
    {
        if (form.Cantidad < 1)
        {
            throw new BadRequestException("La cantidad debe ser al menos 1.");
        }

        if (form.Cantidad > qrCodeGenerator.MaxBatchSize)
        {
            throw new BadRequestException(
                $"La cantidad máxima por lote es {qrCodeGenerator.MaxBatchSize}.");
        }

        var maxFinal = await db.LoteQrs
            .Select(l => (int?)l.ConsecutivoFinal)
            .MaxAsync(cancellationToken) ?? 0;

        var inicio = maxFinal + 1;
        var fin = inicio + form.Cantidad - 1;

        if (fin > qrCodeGenerator.MaxConsecutivo())
        {
            throw new BadRequestException("Se agotó el rango de consecutivos disponibles.");
        }

        ValidatePrintSettings(form.QrSizeCm, form.PaddingCm);

        var now = DateTime.UtcNow;
        var lote = new LoteQr
        {
            ConsecutivoInicial = inicio,
            ConsecutivoFinal = fin,
            Cantidad = form.Cantidad,
            Descripcion = form.Descripcion.Trim(),
            QrSizeCm = form.QrSizeCm,
            PaddingCm = form.PaddingCm,
            LabelPosition = ToEntityLabel(form.LabelPosition),
            MostrarBordes = form.MostrarBordes,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.LoteQrs.Add(lote);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("GENERATE_QR_BATCH", lote.Id, cancellationToken);
        return ToResponse(lote);
    }

    public async Task<LoteQrResponse> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var lote = await db.LoteQrs.AsNoTracking()
            .FirstOrDefaultAsync(l => l.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Lote QR not found: {id}");
        return ToResponse(lote);
    }

    public async Task<IReadOnlyList<LoteQrResponse>> FindAllAsync(CancellationToken cancellationToken = default)
    {
        var lotes = await db.LoteQrs.AsNoTracking()
            .OrderByDescending(l => l.CreatedAt)
            .ToListAsync(cancellationToken);
        return lotes.Select(ToResponse).ToList();
    }

    public async Task<LoteQr> GetEntityAsync(long id, CancellationToken cancellationToken = default)
    {
        return await db.LoteQrs.FirstOrDefaultAsync(l => l.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Lote QR not found: {id}");
    }

    public IReadOnlyList<string> ListCodigos(LoteQr lote) =>
        qrCodeGenerator.FormatRange(lote.ConsecutivoInicial, lote.ConsecutivoFinal);

    private static void ValidatePrintSettings(decimal squareSizeCm, decimal paddingCm)
    {
        if (squareSizeCm <= 2 * paddingCm)
        {
            throw new BadRequestException(
                "El tamaño del cuadrado debe ser mayor que el doble del padding interno.");
        }
    }

    public static void ValidatePrintSettingsOrThrow(decimal squareSizeCm, decimal paddingCm) =>
        ValidatePrintSettings(squareSizeCm, paddingCm);

    private static LoteQrResponse ToResponse(LoteQr lote) =>
        new(
            lote.Id,
            lote.ConsecutivoInicial,
            lote.ConsecutivoFinal,
            lote.Descripcion,
            lote.Cantidad,
            lote.QrSizeCm,
            lote.PaddingCm,
            ToDtoLabel(lote.LabelPosition),
            lote.MostrarBordes,
            lote.CreatedAt,
            lote.UpdatedAt);

    private static EntityLabelPosition ToEntityLabel(DtoLabelPosition position) =>
        Enum.Parse<EntityLabelPosition>(position.ToString());

    private static DtoLabelPosition ToDtoLabel(EntityLabelPosition position) =>
        Enum.Parse<DtoLabelPosition>(position.ToString());
}
