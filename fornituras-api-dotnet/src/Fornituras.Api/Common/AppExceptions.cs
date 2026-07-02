namespace Fornituras.Api.Common;

public abstract class AppException : Exception
{
    protected AppException(string message) : base(message) { }
}

public sealed class NotFoundException(string message) : AppException(message);
public sealed class BadRequestException(string message) : AppException(message);
public sealed class ConflictException(string message) : AppException(message);
public sealed class UnauthorizedAppException(string message) : AppException(message);
public sealed class TooManyRequestsException(string message) : AppException(message);
