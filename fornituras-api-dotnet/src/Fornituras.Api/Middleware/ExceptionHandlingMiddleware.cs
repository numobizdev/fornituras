using System.Net;
using System.Text.Json;
using Fornituras.Api.Common;

namespace Fornituras.Api.Middleware;

/// <summary>
/// Convierte excepciones de dominio en respuestas JSON con el envoltorio ApiResponse.
/// </summary>
public sealed class ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (Exception ex)
        {
            await HandleExceptionAsync(context, ex);
        }
    }

    private async Task HandleExceptionAsync(HttpContext context, Exception exception)
    {
        var (statusCode, message) = exception switch
        {
            NotFoundException notFound => (HttpStatusCode.NotFound, notFound.Message),
            BadRequestException badRequest => (HttpStatusCode.BadRequest, badRequest.Message),
            ConflictException conflict => (HttpStatusCode.Conflict, conflict.Message),
            UnauthorizedAppException unauthorized => (HttpStatusCode.Unauthorized, unauthorized.Message),
            ForbiddenException forbidden => (HttpStatusCode.Forbidden, forbidden.Message),
            PayloadTooLargeException tooLarge => (HttpStatusCode.RequestEntityTooLarge, tooLarge.Message),
            UnprocessableEntityException unprocessable => (HttpStatusCode.UnprocessableEntity, unprocessable.Message),
            TooManyRequestsException tooMany => (HttpStatusCode.TooManyRequests, tooMany.Message),
            NotImplementedException => (HttpStatusCode.NotImplemented, "Funcionalidad pendiente de implementación."),
            _ => (HttpStatusCode.InternalServerError, "Error interno del servidor.")
        };

        if (statusCode == HttpStatusCode.InternalServerError)
        {
            logger.LogError(exception, "Unhandled exception");
        }

        context.Response.ContentType = "application/json";
        context.Response.StatusCode = (int)statusCode;

        var payload = ApiResponse<object>.Error(message);
        await context.Response.WriteAsync(JsonSerializer.Serialize(payload, JsonOptions));
    }
}
