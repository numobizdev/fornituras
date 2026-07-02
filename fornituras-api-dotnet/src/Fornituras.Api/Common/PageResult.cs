namespace Fornituras.Api.Common;

public sealed class PageResult<T>
{
    public IReadOnlyList<T> Content { get; init; } = [];
    public long TotalElements { get; init; }
    public int TotalPages { get; init; }
    public int Number { get; init; }
    public int Size { get; init; }

    public static PageResult<T> From(IReadOnlyList<T> content, long total, int page, int size)
    {
        var totalPages = size <= 0 ? 0 : (int)Math.Ceiling(total / (double)size);
        return new PageResult<T>
        {
            Content = content,
            TotalElements = total,
            TotalPages = totalPages,
            Number = page,
            Size = size
        };
    }
}
