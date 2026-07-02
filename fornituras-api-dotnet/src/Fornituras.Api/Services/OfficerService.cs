using Fornituras.Api.Common;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;

namespace Fornituras.Api.Services;

public sealed class OfficerService(
    ApplicationDbContext db,
    CatalogService catalogService,
    BlindIndexer blindIndexer,
    CurrentUserService currentUser,
    IAuditWriter audit) : IOfficerService
{
    public Task<PageResult<OfficerSummary>> FindAllAsync(
        string? q,
        string? municipio,
        long? sexoId,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindAllInternalAsync(q, municipio, sexoId, pagination.Page, pagination.Size, cancellationToken);

    private async Task<PageResult<OfficerSummary>> FindAllInternalAsync(
        string? q,
        string? municipio,
        long? sexoId,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Officers.AsNoTracking().Where(o => o.Active);

        if (!string.IsNullOrWhiteSpace(q))
        {
            var normalizedPlaca = CodeNormalizer.Normalize(q);
            var curpIdx = blindIndexer.Index(q);
            var rfcIdx = blindIndexer.Index(q);

            query = query.Where(o =>
                o.PlacaNormalizada.Contains(normalizedPlaca) ||
                (curpIdx != null && o.CurpIdx == curpIdx) ||
                (rfcIdx != null && o.RfcIdx == rfcIdx));
        }

        if (!string.IsNullOrWhiteSpace(municipio))
        {
            var upper = municipio.Trim().ToUpperInvariant();
            query = query.Where(o => o.Municipio != null && o.Municipio.ToUpper().Contains(upper));
        }

        if (sexoId.HasValue)
        {
            query = query.Where(o => o.SexoId == sexoId.Value);
        }

        query = query.OrderBy(o => o.Placa);
        var total = await query.CountAsync(cancellationToken);
        var officers = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(officers, cancellationToken);
        return PageResult<OfficerSummary>.From(summaries, total, page, size);
    }

    public async Task<OfficerDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default)
    {
        var officer = await db.Officers.AsNoTracking()
            .FirstOrDefaultAsync(o => o.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Officer not found: {id}");

        await audit.RecordAsync("VIEW_OFFICER", officer.Id, cancellationToken);
        return await MapDetailAsync(officer, cancellationToken);
    }

    public async Task<OfficerDetail> CreateAsync(
        OfficerCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        var placaNorm = CodeNormalizer.Normalize(request.Placa);
        if (string.IsNullOrEmpty(placaNorm))
        {
            throw new BadRequestException("La placa es obligatoria.");
        }

        if (await db.Officers.AnyAsync(o => o.PlacaNormalizada == placaNorm, cancellationToken))
        {
            throw new ConflictException($"Ya existe un elemento con la placa '{request.Placa.Trim()}'.");
        }

        await catalogService.RequireActiveItemAsync(request.SexoId, CatalogCodes.Sexo, cancellationToken);
        if (request.TipoSangreId.HasValue)
        {
            await catalogService.RequireActiveItemAsync(
                request.TipoSangreId.Value, CatalogCodes.TipoSangre, cancellationToken);
        }

        var curpIdx = blindIndexer.Index(request.Curp);
        if (curpIdx is not null &&
            await db.Officers.AnyAsync(o => o.CurpIdx == curpIdx, cancellationToken))
        {
            throw new ConflictException("Ya existe un elemento con esa CURP.");
        }

        var rfcIdx = blindIndexer.Index(request.Rfc);
        if (rfcIdx is not null &&
            await db.Officers.AnyAsync(o => o.RfcIdx == rfcIdx, cancellationToken))
        {
            throw new ConflictException("Ya existe un elemento con ese RFC.");
        }

        var now = DateTime.UtcNow;
        var officer = new Officer
        {
            Nombre = PiiCipher.Encrypt(request.Nombre.Trim())!,
            ApellidoPaterno = PiiCipher.Encrypt(request.ApellidoPaterno.Trim())!,
            ApellidoMaterno = PiiCipher.Encrypt(request.ApellidoMaterno?.Trim()),
            Placa = request.Placa.Trim(),
            PlacaNormalizada = placaNorm,
            Curp = PiiCipher.Encrypt(request.Curp?.Trim()),
            CurpIdx = curpIdx,
            Rfc = PiiCipher.Encrypt(request.Rfc?.Trim()),
            RfcIdx = rfcIdx,
            SexoId = request.SexoId,
            TipoSangreId = request.TipoSangreId,
            Municipio = request.Municipio?.Trim(),
            Estado = request.Estado?.Trim(),
            FotoUrl = request.FotoUrl?.Trim(),
            Active = true,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Officers.Add(officer);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_OFFICER", officer.Id, cancellationToken);
        return await MapDetailAsync(officer, cancellationToken);
    }

    private bool CanViewPii() => RolePolicy.CanViewFullPii(currentUser.User);

    private async Task<IReadOnlyList<OfficerSummary>> MapSummariesAsync(
        IReadOnlyList<Officer> officers,
        CancellationToken cancellationToken)
    {
        var sexoIds = officers.Select(o => o.SexoId).Distinct();
        var sangreIds = officers.Where(o => o.TipoSangreId.HasValue).Select(o => o.TipoSangreId!.Value).Distinct();
        var sexoNames = await catalogService.ResolveNamesAsync(sexoIds, cancellationToken);
        var sangreNames = await catalogService.ResolveNamesAsync(sangreIds, cancellationToken);

        return officers.Select(o =>
        {
            var nombre = DecryptName(o);
            return new OfficerSummary(
                o.Id,
                nombre,
                o.Placa,
                sexoNames.GetValueOrDefault(o.SexoId),
                o.TipoSangreId.HasValue ? sangreNames.GetValueOrDefault(o.TipoSangreId.Value) : null,
                o.Municipio,
                o.Estado,
                o.FotoUrl,
                o.Active);
        }).ToList();
    }

    private async Task<OfficerDetail> MapDetailAsync(Officer o, CancellationToken cancellationToken)
    {
        var unmask = CanViewPii();
        var nombre = PiiCipher.Decrypt(o.Nombre) ?? string.Empty;
        var apPat = PiiCipher.Decrypt(o.ApellidoPaterno) ?? string.Empty;
        var apMat = PiiCipher.Decrypt(o.ApellidoMaterno);
        var curp = PiiCipher.Decrypt(o.Curp);
        var rfc = PiiCipher.Decrypt(o.Rfc);

        if (!unmask)
        {
            curp = PiiMasker.Mask(curp);
            rfc = PiiMasker.Mask(rfc);
        }

        var sexoNombre = await catalogService.ResolveNameAsync(o.SexoId, cancellationToken);
        string? sangre = o.TipoSangreId.HasValue
            ? await catalogService.ResolveNameAsync(o.TipoSangreId.Value, cancellationToken)
            : null;

        var nombreCompleto = string.Join(" ",
            new[] { nombre, apPat, apMat }.Where(s => !string.IsNullOrWhiteSpace(s)));

        return new OfficerDetail(
            o.Id,
            nombre,
            apPat,
            apMat,
            nombreCompleto,
            o.Placa,
            o.SexoId,
            sexoNombre,
            o.TipoSangreId,
            sangre,
            o.Municipio,
            o.Estado,
            curp,
            rfc,
            !unmask,
            o.FotoUrl,
            o.Active,
            o.CreatedAt,
            o.UpdatedAt);
    }

    private static string DecryptName(Officer o)
    {
        var parts = new[]
        {
            PiiCipher.Decrypt(o.Nombre),
            PiiCipher.Decrypt(o.ApellidoPaterno),
            PiiCipher.Decrypt(o.ApellidoMaterno)
        }.Where(s => !string.IsNullOrWhiteSpace(s));
        return string.Join(" ", parts);
    }
}
