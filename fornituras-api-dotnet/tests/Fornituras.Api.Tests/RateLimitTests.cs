using Fornituras.Api.Configuration;

namespace Fornituras.Api.Tests;

// Cobertura unitaria aislable de B-1 (rate limiting, ADR 0010). El comportamiento de rechazo (429)
// requiere un host de integración y se valida manualmente (quickstart §B).
public class RateLimitTests
{
    [Fact]
    public void Default_options_match_java_bucket4j_baseline()
    {
        var options = new RateLimitOptions();

        Assert.Equal(30, options.ByCodigo.PermitLimit);
        Assert.Equal(60, options.ByCodigo.WindowSeconds);
        Assert.Equal(60, options.Public.PermitLimit);
        Assert.Equal(60, options.Public.WindowSeconds);
    }

    [Fact]
    public void Policy_names_are_stable()
    {
        Assert.Equal("by-codigo", RateLimitPolicies.ByCodigo);
        Assert.Equal("public", RateLimitPolicies.Public);
    }
}
