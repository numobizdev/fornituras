namespace Fornituras.Api.Common;

public sealed record ApiResponse<T>(bool Success, string Message, T? Data)
{
    public static ApiResponse<T> Ok(T data, string message = "operation successful") =>
        new(true, message, data);

    public static ApiResponse<T> Error(string message, T? data = default) =>
        new(false, message, data);
}
