namespace Fornituras.Api.Services;

/// <summary>
/// Envío de correos. En desarrollo registra en consola; en producción se integraría SMTP.
/// </summary>
public sealed class EmailService(IHostEnvironment environment, ILogger<EmailService> logger)
{
    public Task SendHtmlEmailAsync(
        string to,
        string subject,
        string templateName,
        IReadOnlyDictionary<string, object>? variables = null,
        CancellationToken cancellationToken = default)
    {
        if (environment.IsDevelopment())
        {
            var vars = variables is null
                ? string.Empty
                : string.Join(", ", variables.Select(kv => $"{kv.Key}={kv.Value}"));
            logger.LogInformation(
                "[DEV EMAIL] To={To} Subject={Subject} Template={Template} Vars=[{Vars}]",
                to, subject, templateName, vars);
            return Task.CompletedTask;
        }

        logger.LogWarning(
            "Email no enviado (SMTP no configurado): To={To} Subject={Subject}",
            to, subject);
        return Task.CompletedTask;
    }
}
