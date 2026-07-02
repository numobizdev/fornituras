using Fornituras.Api.Common;
using Fornituras.Api.Dto;

namespace Fornituras.Api.Services;

public sealed class AlertServiceAdapter(AlertService inner) : IAlertService
{
    public Task<IReadOnlyList<AlertItem>> FindVigenciaAlertsAsync(CancellationToken cancellationToken = default) =>
        inner.VigenciaAlertsAsync(cancellationToken);
}

public sealed class DecommissionServiceAdapter(DecommissionService inner) : IDecommissionService
{
    public Task<PageResult<DecommissionSummary>> FindAllAsync(
        DateOnly? fechaDesde,
        DateOnly? fechaHasta,
        long? tipoId,
        long? motivoId,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        inner.FindAllAsync(fechaDesde, fechaHasta, tipoId, motivoId, pagination, cancellationToken);

    public Task<IReadOnlyList<DecommissionReasonItem>> FindReasonsAsync(CancellationToken cancellationToken = default) =>
        inner.FindReasonsAsync(cancellationToken);

    public Task<DecommissionSummary> CreateAsync(DecommissionRequest request, CancellationToken cancellationToken = default) =>
        inner.DecommissionAsync(request, cancellationToken);
}

public sealed class ReportServiceAdapter(ReportService inner) : IReportService
{
    public Task<ReportTotals> GetTotalsAsync(CancellationToken cancellationToken = default) =>
        inner.TotalsAsync(cancellationToken);

    public Task<PageResult<ActiveAssignmentRow>> FindActiveAssignmentsAsync(
        ActiveAssignmentFilter filter,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        inner.ActiveAssignmentsAsync(filter, pagination.Page, pagination.Size, cancellationToken);

    public Task<byte[]> ExportActiveAssignmentsAsync(ActiveAssignmentFilter filter, CancellationToken cancellationToken = default) =>
        Task.FromException<byte[]>(new NotImplementedException("Exportación de asignaciones activas pendiente de implementación."));

    public Task<PageResult<EquipmentSummary>> FindPredefinedReportAsync(
        PredefinedReportType tipo,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        Task.FromException<PageResult<EquipmentSummary>>(new NotImplementedException("Reportes predefinidos pendientes de implementación."));

    public Task<byte[]> ExportPredefinedReportAsync(PredefinedReportType tipo, CancellationToken cancellationToken = default) =>
        Task.FromException<byte[]>(new NotImplementedException("Exportación de reportes predefinidos pendiente de implementación."));
}

public sealed class LandingServiceAdapter(LandingService inner) : ILandingService
{
    public Task<IReadOnlyList<LandingSectionPublic>> FindPublicAsync(CancellationToken cancellationToken = default) =>
        inner.GetPublicAsync(cancellationToken);

    public Task<IReadOnlyList<LandingSectionPublic>> FindHomeAsync(CancellationToken cancellationToken = default) =>
        inner.GetHomeAsync(cancellationToken);

    public Task<IReadOnlyList<LandingSectionAdmin>> FindSectionsAsync(LandingScope scope, CancellationToken cancellationToken = default) =>
        inner.ListAsync(scope, cancellationToken);

    public Task<LandingSectionAdmin> CreateSectionAsync(LandingSectionCreateRequest request, CancellationToken cancellationToken = default) =>
        inner.CreateAsync(request, cancellationToken);

    public Task<LandingSectionAdmin> UpdateSectionAsync(long id, LandingSectionUpdateRequest request, CancellationToken cancellationToken = default) =>
        inner.UpdateAsync(id, request, cancellationToken);

    public Task<LandingSectionAdmin> DeactivateSectionAsync(long id, CancellationToken cancellationToken = default) =>
        inner.DeactivateAsync(id, cancellationToken);

    public Task<LandingSectionAdmin> ActivateSectionAsync(long id, CancellationToken cancellationToken = default) =>
        inner.ActivateAsync(id, cancellationToken);

    public Task<IReadOnlyList<LandingSectionAdmin>> ReorderSectionsAsync(ReorderRequest request, CancellationToken cancellationToken = default) =>
        inner.ReorderAsync(request, cancellationToken);
}
