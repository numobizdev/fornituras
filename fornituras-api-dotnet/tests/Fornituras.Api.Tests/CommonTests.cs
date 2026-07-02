using Fornituras.Api.Common;
using Fornituras.Api.Common.Text;

namespace Fornituras.Api.Tests;

public class CodeNormalizerTests
{
    [Theory]
    [InlineData("FOR-000042", "FOR000042")]
    [InlineData("  for-123  ", "FOR123")]
    public void Normalize_removes_spaces_and_hyphens(string input, string expected)
    {
        Assert.Equal(expected, CodeNormalizer.Normalize(input));
    }
}

public class ApiResponseTests
{
    [Fact]
    public void Ok_wraps_data_with_success_true()
    {
        var response = ApiResponse<string>.Ok("hello", "done");
        Assert.True(response.Success);
        Assert.Equal("done", response.Message);
        Assert.Equal("hello", response.Data);
    }
}
