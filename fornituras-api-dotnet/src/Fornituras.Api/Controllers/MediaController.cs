using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Fornituras.Api.Controllers;

/// <summary>
/// Fotos (017). Siempre autenticado (nunca anónimo). La subida sanea y cifra; la descarga devuelve
/// el binario descifrado para que el cliente lo pinte vía blob (el <c>&lt;img&gt;</c> no envía token).
/// El control de acceso PII vive en <see cref="IMediaService"/>.
/// </summary>
[ApiController]
[Authorize]
[Route("api/v1/media")]
public sealed class MediaController(IMediaService mediaService) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<ApiResponse<MediaUploadResponse>>> Upload(
        [FromForm] IFormFile? image,
        [FromForm] string? context,
        CancellationToken cancellationToken)
    {
        if (image is null || image.Length == 0)
        {
            throw new BadRequestException("La imagen es obligatoria.");
        }

        using var buffer = new MemoryStream();
        await using (var stream = image.OpenReadStream())
        {
            await stream.CopyToAsync(buffer, cancellationToken);
        }

        var result = await mediaService.UploadAsync(buffer.ToArray(), context ?? string.Empty, cancellationToken);
        return StatusCode(StatusCodes.Status201Created,
            ApiResponse<MediaUploadResponse>.Ok(result, "Foto subida."));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Get(Guid id, CancellationToken cancellationToken)
    {
        var (bytes, contentType) = await mediaService.LoadAsync(id, cancellationToken);
        return File(bytes, contentType);
    }

    [HttpDelete("{id:guid}")]
    public async Task<ActionResult<ApiResponse<object?>>> Delete(Guid id, CancellationToken cancellationToken)
    {
        await mediaService.DeleteAsync(id, cancellationToken);
        return Ok(ApiResponse<object?>.Ok(null, "Foto eliminada."));
    }
}
