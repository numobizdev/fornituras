using Fornituras.Api.Common;
using Fornituras.Api.Dto;

namespace Fornituras.Api.Services;

public interface IAuthService
{
    Task<AuthResponse> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default);
    Task ActivateAccountAsync(ActivateAccountRequest request, CancellationToken cancellationToken = default);
    Task ChangePasswordAsync(ChangePasswordRequest request, CancellationToken cancellationToken = default);
    Task ForgotPasswordAsync(ForgotPasswordRequest request, CancellationToken cancellationToken = default);
    Task ResetPasswordAsync(ResetPasswordRequest request, CancellationToken cancellationToken = default);
}

public interface IUserService
{
    Task<UserResponse> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<PageResult<UserResponse>> FindAllAsync(
        PaginationQuery pagination,
        string? role = null,
        bool? enabled = null,
        CancellationToken cancellationToken = default);
    Task<UserResponse> CreateAsync(UserRequest request, CancellationToken cancellationToken = default);
    Task<UserResponse> UpdateAsync(long id, UserUpdateRequest request, CancellationToken cancellationToken = default);
    Task<UserResponse> SetEnabledAsync(long id, bool enabled, CancellationToken cancellationToken = default);
    Task<UserResponse> ChangeRoleAsync(long id, string role, CancellationToken cancellationToken = default);
    Task<bool> IsCurrentUserAsync(long id, CancellationToken cancellationToken = default);
}

public interface ICatalogService
{
    Task<IReadOnlyList<CatalogSummary>> FindCatalogsAsync(CancellationToken cancellationToken = default);
    Task<PageResult<CatalogItemSummary>> FindItemsAsync(string code, bool? active, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<CatalogItemSummary> FindItemAsync(long itemId, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<CatalogItemSummary>> FindActiveItemsAsync(string code, long? parentItemId, CancellationToken cancellationToken = default);
    Task<CatalogItemSummary> CreateItemAsync(string code, CatalogItemCreateRequest request, CancellationToken cancellationToken = default);
    Task<CatalogItemSummary> UpdateItemAsync(long itemId, CatalogItemCreateRequest request, CancellationToken cancellationToken = default);
    Task DeactivateItemAsync(long itemId, CancellationToken cancellationToken = default);
}

public interface IWarehouseService
{
    Task<PageResult<WarehouseSummary>> FindAllAsync(bool? active, long? tipoItemId, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<WarehouseDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<WarehouseDetail> CreateAsync(WarehouseCreateRequest request, CancellationToken cancellationToken = default);
    Task<WarehouseDetail> UpdateAsync(long id, WarehouseCreateRequest request, CancellationToken cancellationToken = default);
    Task DeactivateAsync(long id, CancellationToken cancellationToken = default);
    Task DeleteAsync(long id, CancellationToken cancellationToken = default);
}

public interface IEquipmentService
{
    Task<PageResult<EquipmentSummary>> FindAllAsync(string? q, EquipmentStatus? status, long? equipmentTypeId, long? sizeId, long? warehouseId, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<EquipmentDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<EquipmentDetail> FindByCodigoAsync(string codigo, CancellationToken cancellationToken = default);
    Task<EquipmentDetail> CreateAsync(EquipmentCreateRequest request, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<EquipmentDetail>> CreateBatchAsync(BatchCreateRequest request, CancellationToken cancellationToken = default);
    Task<EquipmentDetail> UpdateAsync(long id, EquipmentCreateRequest request, CancellationToken cancellationToken = default);
    Task<EquipmentDetail> ChangeStatusAsync(long id, StatusChangeRequest request, CancellationToken cancellationToken = default);
}

public interface IOfficerService
{
    Task<PageResult<OfficerSummary>> FindAllAsync(string? q, string? municipio, long? sexoId, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<OfficerDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<OfficerDetail> CreateAsync(OfficerCreateRequest request, CancellationToken cancellationToken = default);
}

public interface IAssignmentService
{
    Task<PageResult<AssignmentSummary>> FindAllAsync(PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<AssignmentSummary> AssignAsync(AssignRequest request, CancellationToken cancellationToken = default);
    Task<AssignmentSummary> ReturnAsync(long id, CancellationToken cancellationToken = default);
    Task<AssignmentSummary> ReassignAsync(ReassignRequest request, CancellationToken cancellationToken = default);
}

public interface IQrService
{
    Task<LoteQrResponse> GenerateLoteAsync(GenerateQrForm form, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<LoteQrResponse>> FindLotesAsync(CancellationToken cancellationToken = default);
    Task<LoteQrResponse> FindLoteByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<CodigoQrResponse>> FindCodigosAsync(long loteId, CancellationToken cancellationToken = default);
    Task<byte[]> ExportPdfAsync(long loteId, CancellationToken cancellationToken = default);
    Task<byte[]> ExportZipAsync(long loteId, CancellationToken cancellationToken = default);
    Task<byte[]> ReprintPdfAsync(long loteId, ReprintQrForm form, CancellationToken cancellationToken = default);
    Task<byte[]> ReprintZipAsync(long loteId, ReprintQrForm form, CancellationToken cancellationToken = default);
}

public interface IDashboardService
{
    Task<DashboardSummary> GetSummaryAsync(CancellationToken cancellationToken = default);
}

public interface ITransferService
{
    Task<PageResult<TransferSummary>> FindAllAsync(long? origenId, long? destinoId, TransferStatus? status, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<TransferDetail> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<TransferDetail> CreateAsync(TransferCreateRequest request, CancellationToken cancellationToken = default);
    Task<TransferDetail> ReceiveAsync(long id, CancellationToken cancellationToken = default);
    Task<TransferDetail> CancelAsync(long id, CancellationToken cancellationToken = default);
}

public interface IIncidentService
{
    Task<PageResult<IncidentSummary>> FindAllAsync(IncidentStatus? estado, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<IncidentSummary> FindByIdAsync(long id, CancellationToken cancellationToken = default);
    Task<IncidentSummary> CreateAsync(IncidentCreateRequest request, CancellationToken cancellationToken = default);
    Task<IncidentSummary> UpdateAsync(long id, IncidentUpdateRequest request, CancellationToken cancellationToken = default);
}

public interface IAlertService
{
    Task<IReadOnlyList<AlertItem>> FindVigenciaAlertsAsync(CancellationToken cancellationToken = default);
}

public interface IDecommissionService
{
    Task<PageResult<DecommissionSummary>> FindAllAsync(DateOnly? fechaDesde, DateOnly? fechaHasta, long? tipoId, long? motivoId, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<DecommissionReasonItem>> FindReasonsAsync(CancellationToken cancellationToken = default);
    Task<DecommissionSummary> CreateAsync(DecommissionRequest request, CancellationToken cancellationToken = default);
}

public interface IReportService
{
    Task<ReportTotals> GetTotalsAsync(CancellationToken cancellationToken = default);
    Task<PageResult<ActiveAssignmentRow>> FindActiveAssignmentsAsync(ActiveAssignmentFilter filter, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<byte[]> ExportActiveAssignmentsAsync(ActiveAssignmentFilter filter, CancellationToken cancellationToken = default);
    Task<PageResult<EquipmentSummary>> FindPredefinedReportAsync(PredefinedReportType tipo, PaginationQuery pagination, CancellationToken cancellationToken = default);
    Task<byte[]> ExportPredefinedReportAsync(PredefinedReportType tipo, CancellationToken cancellationToken = default);
}

public interface IAuditService
{
    Task<PageResult<AuditLogSummary>> FindAllAsync(string? actor, string? accion, string? entidad, DateTime? desde, DateTime? hasta, PaginationQuery pagination, CancellationToken cancellationToken = default);
}

public interface ILandingService
{
    Task<IReadOnlyList<LandingSectionPublic>> FindPublicAsync(CancellationToken cancellationToken = default);
    Task<IReadOnlyList<LandingSectionPublic>> FindHomeAsync(CancellationToken cancellationToken = default);
    Task<IReadOnlyList<LandingSectionAdmin>> FindSectionsAsync(LandingScope scope, CancellationToken cancellationToken = default);
    Task<LandingSectionAdmin> CreateSectionAsync(LandingSectionCreateRequest request, CancellationToken cancellationToken = default);
    Task<LandingSectionAdmin> UpdateSectionAsync(long id, LandingSectionUpdateRequest request, CancellationToken cancellationToken = default);
    Task<LandingSectionAdmin> DeactivateSectionAsync(long id, CancellationToken cancellationToken = default);
    Task<LandingSectionAdmin> ActivateSectionAsync(long id, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<LandingSectionAdmin>> ReorderSectionsAsync(ReorderRequest request, CancellationToken cancellationToken = default);
}
