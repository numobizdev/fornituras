using Fornituras.Api.Common;
using Fornituras.Api.Dto;

namespace Fornituras.Api.Services;

public sealed class AuditService(AuditLogService auditLogService) : IAuditService
{
    public Task<PageResult<AuditLogSummary>> FindAllAsync(
        string? actor,
        string? accion,
        string? entidad,
        DateTime? desde,
        DateTime? hasta,
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        auditLogService.QueryAsync(
            actor, accion, entidad, desde, hasta,
            pagination.Page, pagination.Size, cancellationToken);
}
