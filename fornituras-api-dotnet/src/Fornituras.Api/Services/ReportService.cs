using Fornituras.Api.Common;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Common.Text;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityIncidentStatus = Fornituras.Api.Data.Entities.IncidentStatus;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class ReportService(
    ApplicationDbContext db,
    BlindIndexer blindIndexer,
    CurrentUserService currentUser,
    IAuditWriter audit) : IReportService
{
    private static readonly EntityIncidentStatus[] ActiveIncidents =
    [
        EntityIncidentStatus.ABIERTA,
        EntityIncidentStatus.EN_PROCESO
    ];

    public Task<ReportTotals> GetTotalsAsync(CancellationToken cancellationToken = default) =>
        TotalsAsync(cancellationToken);

    public async Task<ReportTotals> TotalsAsync(CancellationToken cancellationToken = default)
    {
        var statusCounts = await db.Equipment.AsNoTracking()
            .GroupBy(e => e.Status)
            .Select(g => new { Status = g.Key, Count = g.LongCount() })
            .ToListAsync(cancellationToken);

        var totalFornituras = statusCounts.Sum(x => x.Count);
        var disponibles = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.DISPONIBLE)?.Count ?? 0;
        var asignadas = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.ASIGNADA)?.Count ?? 0;
        var enMantenimiento = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.EN_MANTENIMIENTO)?.Count ?? 0;
        var baja = statusCounts.FirstOrDefault(x => x.Status == EntityEquipmentStatus.BAJA_DEFINITIVA)?.Count ?? 0;

        var conIncidencia = await db.Incidents.AsNoTracking()
            .Where(i => ActiveIncidents.Contains(i.Estado))
            .Select(i => i.EquipmentId)
            .Distinct()
            .LongCountAsync(cancellationToken);

        var totalElementos = await db.Officers.AsNoTracking()
            .CountAsync(o => o.Active, cancellationToken);

        return new ReportTotals(
            totalFornituras,
            disponibles,
            asignadas,
            enMantenimiento,
            conIncidencia,
            baja,
            totalElementos);
    }

    public Task<PageResult<ActiveAssignmentRow>> FindActiveAssignmentsAsync(
        ActiveAssignmentFilter filter,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        ActiveAssignmentsAsync(filter, pagination.Page, pagination.Size, cancellationToken);

    public async Task<PageResult<ActiveAssignmentRow>> ActiveAssignmentsAsync(
        ActiveAssignmentFilter filter,
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var rows = await ActiveAssignmentRowsAsync(filter, cancellationToken);
        var total = rows.Count;
        var pageRows = rows.Skip(page * size).Take(size).ToList();
        return PageResult<ActiveAssignmentRow>.From(pageRows, total, page, size);
    }

    public async Task<IReadOnlyList<ActiveAssignmentRow>> ActiveAssignmentRowsAsync(
        ActiveAssignmentFilter filter,
        CancellationToken cancellationToken = default)
    {
        var query = from a in db.Assignments.AsNoTracking()
                    join e in db.Equipment.AsNoTracking() on a.EquipmentId equals e.Id
                    join o in db.Officers.AsNoTracking() on a.OfficerId equals o.Id
                    where a.FechaDevolucion == null
                    select new { Assignment = a, Equipment = e, Officer = o };

        if (!string.IsNullOrWhiteSpace(filter.Qr))
        {
            var norm = CodeNormalizer.Normalize(filter.Qr);
            query = query.Where(x => x.Equipment.CodigoNormalizado.Contains(norm));
        }

        if (!string.IsNullOrWhiteSpace(filter.Placa))
        {
            var norm = CodeNormalizer.Normalize(filter.Placa);
            query = query.Where(x => x.Officer.PlacaNormalizada.Contains(norm));
        }

        if (!string.IsNullOrWhiteSpace(filter.Curp))
        {
            var idx = blindIndexer.Index(filter.Curp);
            if (idx is not null)
            {
                query = query.Where(x => x.Officer.CurpIdx == idx);
            }
        }

        if (!string.IsNullOrWhiteSpace(filter.Rfc))
        {
            var idx = blindIndexer.Index(filter.Rfc);
            if (idx is not null)
            {
                query = query.Where(x => x.Officer.RfcIdx == idx);
            }
        }

        if (!string.IsNullOrWhiteSpace(filter.Municipio))
        {
            var upper = filter.Municipio.Trim().ToUpperInvariant();
            query = query.Where(x =>
                x.Officer.Municipio != null && x.Officer.Municipio.ToUpper().Contains(upper));
        }

        var results = await query.OrderByDescending(x => x.Assignment.FechaAsignacion).ToListAsync(cancellationToken);

        if (!string.IsNullOrWhiteSpace(filter.Nombre))
        {
            var upperNombre = filter.Nombre.Trim().ToUpperInvariant();
            results = results.Where(x =>
            {
                var full = BuildOfficerName(x.Officer).ToUpperInvariant();
                return full.Contains(upperNombre);
            }).ToList();
        }

        var masked = !RolePolicy.CanViewFullPii(currentUser.User);

        return results.Select(x =>
        {
            var curp = PiiCipher.Decrypt(x.Officer.Curp);
            var rfc = PiiCipher.Decrypt(x.Officer.Rfc);
            if (masked)
            {
                curp = PiiMasker.Mask(curp);
                rfc = PiiMasker.Mask(rfc);
            }

            return new ActiveAssignmentRow(
                x.Assignment.Id,
                x.Equipment.Id,
                x.Equipment.CodigoQr,
                x.Equipment.Descripcion,
                x.Officer.Id,
                BuildOfficerName(x.Officer),
                x.Officer.Placa,
                curp,
                rfc,
                x.Officer.Municipio,
                x.Officer.Estado,
                masked,
                x.Assignment.FechaAsignacion);
        }).ToList();
    }

    public bool PiiMaskedForCurrentActor() => !RolePolicy.CanViewFullPii(currentUser.User);

    public async Task<byte[]> ExportActiveAssignmentsAsync(
        ActiveAssignmentFilter filter,
        CancellationToken cancellationToken = default)
    {
        var rows = await ActiveAssignmentRowsAsync(filter, cancellationToken);
        // Auditar la exportación (FR-005) sin volcar PII: solo el volumen exportado.
        await audit.RecordEventAsync("EXPORT_ACTIVE_ASSIGNMENTS", $"rows={rows.Count}", cancellationToken);
        var lines = new List<string> { "QR,Elemento,Placa,CURP,RFC,Municipio,FechaAsignacion" };
        foreach (var row in rows)
        {
            lines.Add($"{row.CodigoQr},{row.ElementoNombre},{row.Placa},{row.Curp},{row.Rfc},{row.Municipio},{row.FechaAsignacion:O}");
        }

        return System.Text.Encoding.UTF8.GetBytes(string.Join(Environment.NewLine, lines));
    }

    public async Task<PageResult<EquipmentSummary>> FindPredefinedReportAsync(
        PredefinedReportType tipo,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default)
    {
        Dto.EquipmentStatus? status = tipo switch
        {
            PredefinedReportType.DISPONIBLES => Dto.EquipmentStatus.DISPONIBLE,
            PredefinedReportType.ASIGNADAS => Dto.EquipmentStatus.ASIGNADA,
            PredefinedReportType.MANTENIMIENTO => Dto.EquipmentStatus.EN_MANTENIMIENTO,
            PredefinedReportType.BAJA => Dto.EquipmentStatus.BAJA_DEFINITIVA,
            _ => null
        };

        // Simplified: reuse equipment query via direct DB for predefined reports
        var query = db.Equipment.AsNoTracking();
        if (status.HasValue)
        {
            query = query.Where(e => e.Status == Enum.Parse<Data.Entities.EquipmentStatus>(status.Value.ToString()));
        }

        query = query.OrderBy(e => e.CodigoQr);
        var total = await query.CountAsync(cancellationToken);
        var items = await query.Skip(pagination.Page * pagination.Size).Take(pagination.Size).ToListAsync(cancellationToken);

        var tipoIds = items.Select(i => i.EquipmentTypeId).Distinct();
        var tipoNames = await db.CatalogItems.AsNoTracking()
            .Where(c => tipoIds.Contains(c.Id))
            .ToDictionaryAsync(c => c.Id, c => c.Nombre, cancellationToken);
        var warehouseNames = await db.Warehouses.AsNoTracking()
            .Where(w => items.Select(i => i.WarehouseId).Contains(w.Id))
            .ToDictionaryAsync(w => w.Id, w => w.Nombre, cancellationToken);

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var summaries = items.Select(e =>
        {
            var vigencia = ExpiryCalculator.StatusFor(e.FechaVencimiento, today);
            return new EquipmentSummary(
                e.Id,
                e.CodigoQr,
                e.Descripcion,
                tipoNames.GetValueOrDefault(e.EquipmentTypeId, "?"),
                null,
                warehouseNames.GetValueOrDefault(e.WarehouseId, "?"),
                Enum.Parse<Dto.EquipmentStatus>(e.Status.ToString()),
                vigencia.HasValue ? Enum.Parse<Dto.ExpiryStatus>(vigencia.Value.ToString()) : Dto.ExpiryStatus.VIGENTE,
                e.FechaVencimiento);
        }).ToList();

        return PageResult<EquipmentSummary>.From(summaries, total, pagination.Page, pagination.Size);
    }

    public async Task<byte[]> ExportPredefinedReportAsync(
        PredefinedReportType tipo,
        CancellationToken cancellationToken = default)
    {
        var page = await FindPredefinedReportAsync(tipo, new PaginationQuery { Page = 0, Size = 10_000 }, cancellationToken);
        await audit.RecordEventAsync("EXPORT_PREDEFINED_REPORT", tipo.ToString(), cancellationToken);
        var lines = new List<string> { "Codigo,Descripcion,Tipo,Almacen,Status" };
        foreach (var row in page.Content)
        {
            lines.Add($"{row.CodigoQr},{row.Descripcion},{row.TipoNombre},{row.AlmacenNombre},{row.Status}");
        }

        return System.Text.Encoding.UTF8.GetBytes(string.Join(Environment.NewLine, lines));
    }

    private static string BuildOfficerName(Officer officer)
    {
        var parts = new[]
        {
            PiiCipher.Decrypt(officer.Nombre),
            PiiCipher.Decrypt(officer.ApellidoPaterno),
            PiiCipher.Decrypt(officer.ApellidoMaterno)
        }.Where(s => !string.IsNullOrWhiteSpace(s));
        return string.Join(" ", parts);
    }
}
