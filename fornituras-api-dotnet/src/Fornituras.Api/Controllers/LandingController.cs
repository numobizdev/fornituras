using Fornituras.Api.Common;
using Fornituras.Api.Dto;
using Fornituras.Api.Configuration;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace Fornituras.Api.Controllers;

[ApiController]
[Route("api/v1/landing")]
public sealed class LandingController(ILandingService landingService) : ControllerBase
{
    [AllowAnonymous]
    [EnableRateLimiting(RateLimitPolicies.Public)]
    [HttpGet("public")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<LandingSectionPublic>>>> GetPublic(
        CancellationToken cancellationToken)
    {
        var sections = await landingService.FindPublicAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<LandingSectionPublic>>.Ok(sections));
    }

    [Authorize]
    [HttpGet("home")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<LandingSectionPublic>>>> GetHome(
        CancellationToken cancellationToken)
    {
        var sections = await landingService.FindHomeAsync(cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<LandingSectionPublic>>.Ok(sections));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpGet("sections")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<LandingSectionAdmin>>>> GetSections(
        [FromQuery] LandingScope scope,
        CancellationToken cancellationToken)
    {
        var sections = await landingService.FindSectionsAsync(scope, cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<LandingSectionAdmin>>.Ok(sections));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpPost("sections")]
    public async Task<ActionResult<ApiResponse<LandingSectionAdmin>>> CreateSection(
        [FromBody] LandingSectionCreateRequest request,
        CancellationToken cancellationToken)
    {
        var created = await landingService.CreateSectionAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiResponse<LandingSectionAdmin>.Ok(created));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpPut("sections/{id:long}")]
    public async Task<ActionResult<ApiResponse<LandingSectionAdmin>>> UpdateSection(
        long id,
        [FromBody] LandingSectionUpdateRequest request,
        CancellationToken cancellationToken)
    {
        var updated = await landingService.UpdateSectionAsync(id, request, cancellationToken);
        return Ok(ApiResponse<LandingSectionAdmin>.Ok(updated));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpPatch("sections/{id:long}/deactivate")]
    public async Task<ActionResult<ApiResponse<LandingSectionAdmin>>> DeactivateSection(
        long id,
        CancellationToken cancellationToken)
    {
        var section = await landingService.DeactivateSectionAsync(id, cancellationToken);
        return Ok(ApiResponse<LandingSectionAdmin>.Ok(section));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpPatch("sections/{id:long}/activate")]
    public async Task<ActionResult<ApiResponse<LandingSectionAdmin>>> ActivateSection(
        long id,
        CancellationToken cancellationToken)
    {
        var section = await landingService.ActivateSectionAsync(id, cancellationToken);
        return Ok(ApiResponse<LandingSectionAdmin>.Ok(section));
    }

    [Authorize(Roles = RolePolicy.ManageLanding)]
    [HttpPatch("sections/reorder")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<LandingSectionAdmin>>>> ReorderSections(
        [FromBody] ReorderRequest request,
        CancellationToken cancellationToken)
    {
        var sections = await landingService.ReorderSectionsAsync(request, cancellationToken);
        return Ok(ApiResponse<IReadOnlyList<LandingSectionAdmin>>.Ok(sections));
    }
}
