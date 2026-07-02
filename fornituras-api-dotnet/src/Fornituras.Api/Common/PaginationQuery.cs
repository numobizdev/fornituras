namespace Fornituras.Api.Common;

/// <summary>
/// Parámetros de paginación compatibles con Spring Pageable (page/size).
/// </summary>
public sealed class PaginationQuery
{
    public int Page { get; init; }
    public int Size { get; init; } = 20;
}
