namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Sección de contenido configurable de la landing (pública o panel autenticado).
/// </summary>
public class LandingSection : BaseEntity
{
    public LandingScope Scope { get; set; }

    public LandingSectionType Type { get; set; }

    public string? Titulo { get; set; }

    public string? Subtitulo { get; set; }

    public string? Cuerpo { get; set; }

    public string? ImagenUrl { get; set; }

    public string? CtaLabel { get; set; }

    public string? CtaUrl { get; set; }

    public int Orden { get; set; }

    public bool Active { get; set; } = true;

    /// <summary>JSON de accesos rápidos para <see cref="LandingSectionType.QUICK_LINKS"/>.</summary>
    public string? ConfigJson { get; set; }
}
