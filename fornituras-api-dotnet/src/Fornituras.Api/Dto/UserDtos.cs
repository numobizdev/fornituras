namespace Fornituras.Api.Dto;

public sealed record UserRequest(string Name, string Email, string Role);

public sealed record UserUpdateRequest(string Name);

public sealed record UserResponse(
    long Id,
    string Name,
    string Email,
    string Role,
    bool Enabled,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record EnabledUpdateRequest(bool Enabled);

public sealed record RoleUpdateRequest(string Role);
