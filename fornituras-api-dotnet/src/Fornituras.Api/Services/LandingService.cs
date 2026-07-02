using System.Text.Json;
using Fornituras.Api.Common;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Microsoft.EntityFrameworkCore;
using EntityLandingScope = Fornituras.Api.Data.Entities.LandingScope;
using EntityLandingSectionType = Fornituras.Api.Data.Entities.LandingSectionType;
using DtoLandingScope = Fornituras.Api.Dto.LandingScope;
using DtoLandingSectionType = Fornituras.Api.Dto.LandingSectionType;

namespace Fornituras.Api.Services;

public sealed class LandingService(ApplicationDbContext db, IAuditWriter audit) : ILandingService
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public Task<IReadOnlyList<LandingSectionPublic>> FindPublicAsync(CancellationToken cancellationToken = default) =>
        GetPublicAsync(cancellationToken);

    public Task<IReadOnlyList<LandingSectionPublic>> FindHomeAsync(CancellationToken cancellationToken = default) =>
        GetHomeAsync(cancellationToken);

    public Task<IReadOnlyList<LandingSectionAdmin>> FindSectionsAsync(
        DtoLandingScope scope,
        CancellationToken cancellationToken = default) =>
        ListAsync(scope, cancellationToken);

    public Task<LandingSectionAdmin> CreateSectionAsync(
        LandingSectionCreateRequest request,
        CancellationToken cancellationToken = default) =>
        CreateAsync(request, cancellationToken);

    public Task<LandingSectionAdmin> UpdateSectionAsync(
        long id,
        LandingSectionUpdateRequest request,
        CancellationToken cancellationToken = default) =>
        UpdateAsync(id, request, cancellationToken);

    public Task<LandingSectionAdmin> DeactivateSectionAsync(long id, CancellationToken cancellationToken = default) =>
        DeactivateAsync(id, cancellationToken);

    public Task<LandingSectionAdmin> ActivateSectionAsync(long id, CancellationToken cancellationToken = default) =>
        ActivateAsync(id, cancellationToken);

    public Task<IReadOnlyList<LandingSectionAdmin>> ReorderSectionsAsync(
        ReorderRequest request,
        CancellationToken cancellationToken = default) =>
        ReorderAsync(request, cancellationToken);

    public async Task<IReadOnlyList<LandingSectionPublic>> GetPublicAsync(CancellationToken cancellationToken = default) =>
        await ActiveOfAsync(EntityLandingScope.PUBLIC, cancellationToken);

    public async Task<IReadOnlyList<LandingSectionPublic>> GetHomeAsync(CancellationToken cancellationToken = default) =>
        await ActiveOfAsync(EntityLandingScope.HOME, cancellationToken);

    public async Task<IReadOnlyList<LandingSectionAdmin>> ListAsync(
        DtoLandingScope scope,
        CancellationToken cancellationToken = default)
    {
        var entityScope = ToEntityScope(scope);
        var sections = await db.LandingSections.AsNoTracking()
            .Where(s => s.Scope == entityScope)
            .OrderBy(s => s.Orden)
            .ToListAsync(cancellationToken);
        return sections.Select(ToAdmin).ToList();
    }

    public async Task<LandingSectionAdmin> CreateAsync(
        LandingSectionCreateRequest request,
        CancellationToken cancellationToken = default)
    {
        LandingSectionRules.Validate(
            request.Type, request.Titulo, request.CtaLabel, request.CtaUrl, request.QuickLinks);

        var now = DateTime.UtcNow;
        var section = new LandingSection
        {
            Scope = ToEntityScope(request.Scope),
            Type = ToEntityType(request.Type),
            Titulo = request.Titulo?.Trim(),
            Subtitulo = request.Subtitulo?.Trim(),
            Cuerpo = request.Cuerpo?.Trim(),
            ImagenUrl = request.ImagenUrl?.Trim(),
            CtaLabel = request.CtaLabel?.Trim(),
            CtaUrl = request.CtaUrl?.Trim(),
            Orden = request.Orden,
            Active = true,
            ConfigJson = WriteQuickLinks(request.QuickLinks),
            CreatedAt = now,
            UpdatedAt = now
        };

        db.LandingSections.Add(section);
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("CREATE_LANDING_SECTION", section.Id, cancellationToken);
        return ToAdmin(section);
    }

    public async Task<LandingSectionAdmin> UpdateAsync(
        long id,
        LandingSectionUpdateRequest request,
        CancellationToken cancellationToken = default)
    {
        LandingSectionRules.Validate(
            request.Type, request.Titulo, request.CtaLabel, request.CtaUrl, request.QuickLinks);

        var section = await GetOrThrowAsync(id, cancellationToken);
        section.Scope = ToEntityScope(request.Scope);
        section.Type = ToEntityType(request.Type);
        section.Titulo = request.Titulo?.Trim();
        section.Subtitulo = request.Subtitulo?.Trim();
        section.Cuerpo = request.Cuerpo?.Trim();
        section.ImagenUrl = request.ImagenUrl?.Trim();
        section.CtaLabel = request.CtaLabel?.Trim();
        section.CtaUrl = request.CtaUrl?.Trim();
        section.Orden = request.Orden;
        section.ConfigJson = WriteQuickLinks(request.QuickLinks);
        section.UpdatedAt = DateTime.UtcNow;

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("UPDATE_LANDING_SECTION", section.Id, cancellationToken);
        return ToAdmin(section);
    }

    public async Task<LandingSectionAdmin> DeactivateAsync(long id, CancellationToken cancellationToken = default) =>
        await SetActiveAsync(id, false, "DEACTIVATE_LANDING_SECTION", cancellationToken);

    public async Task<LandingSectionAdmin> ActivateAsync(long id, CancellationToken cancellationToken = default) =>
        await SetActiveAsync(id, true, "ACTIVATE_LANDING_SECTION", cancellationToken);

    public async Task<IReadOnlyList<LandingSectionAdmin>> ReorderAsync(
        ReorderRequest request,
        CancellationToken cancellationToken = default)
    {
        var ids = request.Items.Select(i => i.Id).ToList();
        var sections = await db.LandingSections.Where(s => ids.Contains(s.Id)).ToListAsync(cancellationToken);

        foreach (var item in request.Items)
        {
            var section = sections.FirstOrDefault(s => s.Id == item.Id)
                ?? throw new NotFoundException($"Landing section not found: {item.Id}");
            section.Orden = item.Orden;
            section.UpdatedAt = DateTime.UtcNow;
        }

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordEventAsync("REORDER_LANDING_SECTIONS", request.Items.Count.ToString());
        return sections.OrderBy(s => s.Orden).Select(ToAdmin).ToList();
    }

    private async Task<LandingSectionAdmin> SetActiveAsync(
        long id,
        bool active,
        string action,
        CancellationToken cancellationToken)
    {
        var section = await GetOrThrowAsync(id, cancellationToken);
        section.Active = active;
        section.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync(action, section.Id, cancellationToken);
        return ToAdmin(section);
    }

    private async Task<IReadOnlyList<LandingSectionPublic>> ActiveOfAsync(
        EntityLandingScope scope,
        CancellationToken cancellationToken)
    {
        var sections = await db.LandingSections.AsNoTracking()
            .Where(s => s.Scope == scope && s.Active)
            .OrderBy(s => s.Orden)
            .ToListAsync(cancellationToken);
        return sections.Select(ToPublic).ToList();
    }

    private async Task<LandingSection> GetOrThrowAsync(long id, CancellationToken cancellationToken) =>
        await db.LandingSections.FirstOrDefaultAsync(s => s.Id == id, cancellationToken)
        ?? throw new NotFoundException($"Landing section not found: {id}");

    private static LandingSectionPublic ToPublic(LandingSection section) =>
        new(
            ToDtoType(section.Type),
            section.Titulo,
            section.Subtitulo,
            section.Cuerpo,
            section.ImagenUrl,
            section.CtaLabel,
            section.CtaUrl,
            section.Orden,
            ReadQuickLinks(section.ConfigJson));

    private static LandingSectionAdmin ToAdmin(LandingSection section) =>
        new(
            section.Id,
            ToDtoScope(section.Scope),
            ToDtoType(section.Type),
            section.Titulo,
            section.Subtitulo,
            section.Cuerpo,
            section.ImagenUrl,
            section.CtaLabel,
            section.CtaUrl,
            section.Orden,
            section.Active,
            ReadQuickLinks(section.ConfigJson),
            section.CreatedAt,
            section.UpdatedAt);

    private static string? WriteQuickLinks(IReadOnlyList<QuickLinkItem>? links)
    {
        if (links is null || links.Count == 0)
        {
            return null;
        }

        return JsonSerializer.Serialize(links, JsonOptions);
    }

    private static IReadOnlyList<QuickLinkItem> ReadQuickLinks(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            return [];
        }

        try
        {
            return JsonSerializer.Deserialize<List<QuickLinkItem>>(json, JsonOptions) ?? [];
        }
        catch (JsonException)
        {
            return [];
        }
    }

    private static EntityLandingScope ToEntityScope(DtoLandingScope scope) =>
        Enum.Parse<EntityLandingScope>(scope.ToString());

    private static DtoLandingScope ToDtoScope(EntityLandingScope scope) =>
        Enum.Parse<DtoLandingScope>(scope.ToString());

    private static EntityLandingSectionType ToEntityType(DtoLandingSectionType type) =>
        Enum.Parse<EntityLandingSectionType>(type.ToString());

    private static DtoLandingSectionType ToDtoType(EntityLandingSectionType type) =>
        Enum.Parse<DtoLandingSectionType>(type.ToString());
}

/// <summary>
/// Reglas estructurales de secciones de landing.
/// </summary>
internal static class LandingSectionRules
{
    public static void Validate(
        DtoLandingSectionType type,
        string? titulo,
        string? ctaLabel,
        string? ctaUrl,
        IReadOnlyList<QuickLinkItem>? quickLinks)
    {
        var tituloRequerido = type is
            DtoLandingSectionType.HERO or
            DtoLandingSectionType.ANNOUNCEMENT or
            DtoLandingSectionType.RICH_TEXT;

        if (tituloRequerido && string.IsNullOrWhiteSpace(titulo))
        {
            throw new BadRequestException($"El título es obligatorio para el tipo {type}.");
        }

        if (type == DtoLandingSectionType.QUICK_LINKS)
        {
            if (quickLinks is null || quickLinks.Count == 0)
            {
                throw new BadRequestException("Una sección de accesos rápidos requiere al menos un acceso.");
            }
        }
        else if (quickLinks is { Count: > 0 })
        {
            throw new BadRequestException("Solo las secciones de accesos rápidos admiten accesos.");
        }

        var hasLabel = !string.IsNullOrWhiteSpace(ctaLabel);
        var hasUrl = !string.IsNullOrWhiteSpace(ctaUrl);
        if (hasLabel != hasUrl)
        {
            throw new BadRequestException("La etiqueta y el destino del botón de acción van juntos.");
        }
    }
}
