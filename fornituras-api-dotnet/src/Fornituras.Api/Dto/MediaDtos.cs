namespace Fornituras.Api.Dto;

/// <summary>
/// Respuesta al subir una foto: identificador opaco, referencia interna a guardar en <c>fotoUrl</c>
/// y content-type final tras el saneo.
/// </summary>
public sealed record MediaUploadResponse(Guid Id, string Url, string ContentType);
