namespace Fornituras.Api.Dto;

public enum LandingScope
{
    PUBLIC,
    HOME
}

public enum LandingSectionType
{
    HERO,
    ANNOUNCEMENT,
    QUICK_LINKS,
    RICH_TEXT
}

public sealed record QuickLinkItem(string Label, string Url, string? Icon);

public sealed record LandingSectionAdmin(
    long Id,
    LandingScope Scope,
    LandingSectionType Type,
    string? Titulo,
    string? Subtitulo,
    string? Cuerpo,
    string? ImagenUrl,
    string? CtaLabel,
    string? CtaUrl,
    int Orden,
    bool Active,
    IReadOnlyList<QuickLinkItem> QuickLinks,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record LandingSectionPublic(
    LandingSectionType Type,
    string? Titulo,
    string? Subtitulo,
    string? Cuerpo,
    string? ImagenUrl,
    string? CtaLabel,
    string? CtaUrl,
    int Orden,
    IReadOnlyList<QuickLinkItem> QuickLinks);

public sealed record LandingSectionCreateRequest(
    LandingScope Scope,
    LandingSectionType Type,
    string? Titulo,
    string? Subtitulo,
    string? Cuerpo,
    string? ImagenUrl,
    string? CtaLabel,
    string? CtaUrl,
    int Orden,
    IReadOnlyList<QuickLinkItem>? QuickLinks);

public sealed record LandingSectionUpdateRequest(
    LandingScope Scope,
    LandingSectionType Type,
    string? Titulo,
    string? Subtitulo,
    string? Cuerpo,
    string? ImagenUrl,
    string? CtaLabel,
    string? CtaUrl,
    int Orden,
    IReadOnlyList<QuickLinkItem>? QuickLinks);

public sealed record ReorderRequest(IReadOnlyList<ReorderItem> Items);

public sealed record ReorderItem(long Id, int Orden);
