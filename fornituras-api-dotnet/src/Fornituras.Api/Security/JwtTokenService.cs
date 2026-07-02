using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Text.RegularExpressions;
using Fornituras.Api.Configuration;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace Fornituras.Api.Security;

/// <summary>
/// Emisión y validación de JWT HMAC con subject=email (equivalente a JwtService en Java).
/// </summary>
public sealed class JwtTokenService(IOptions<AppOptions> options)
{
    private readonly JwtOptions _jwt = options.Value.Jwt;

    public long ExpirationMs => _jwt.ExpirationMs;

    public string GenerateToken(string email, string role)
    {
        var key = new SymmetricSecurityKey(ResolveKeyBytes(_jwt.Secret));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, email),
            new Claim(ClaimTypes.Role, role)
        };

        var token = new JwtSecurityToken(
            claims: claims,
            expires: DateTime.UtcNow.AddMilliseconds(_jwt.ExpirationMs),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public ClaimsPrincipal? ValidateToken(string token)
    {
        var handler = new JwtSecurityTokenHandler();
        try
        {
            return handler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuer = false,
                ValidateAudience = false,
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(ResolveKeyBytes(_jwt.Secret)),
                ValidateLifetime = true,
                ClockSkew = TimeSpan.FromMinutes(1)
            }, out _);
        }
        catch
        {
            return null;
        }
    }

    /// <summary>Resuelve la clave: hex → base64 → UTF-8 (igual que Java).</summary>
    internal static byte[] ResolveKeyBytes(string secret)
    {
        if (Regex.IsMatch(secret, "^[0-9A-Fa-f]+$") && secret.Length % 2 == 0)
        {
            return Convert.FromHexString(secret);
        }

        try
        {
            return Convert.FromBase64String(secret);
        }
        catch (FormatException)
        {
            return Encoding.UTF8.GetBytes(secret);
        }
    }
}
