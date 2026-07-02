namespace Fornituras.Api.Configuration;

public sealed class QrOptions
{
    public string Prefix { get; set; } = "FOR-";
    public int SequenceLength { get; set; } = 6;
    public int MaxBatchSize { get; set; } = 10_000;
}
