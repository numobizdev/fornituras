using System.Text.RegularExpressions;
using Fornituras.Api.Common;
using Fornituras.Api.Configuration;
using Microsoft.Extensions.Options;

namespace Fornituras.Api.Services;

public sealed class QrCodeGeneratorService(IOptions<AppOptions> options)
{
    private readonly QrOptions _qr = options.Value.Qr;

    public string Prefix => _qr.Prefix;
    public int SequenceLength => _qr.SequenceLength;
    public int MaxBatchSize => _qr.MaxBatchSize;

    public string FormatCode(int consecutivo)
    {
        if (consecutivo < 1 || consecutivo > MaxConsecutivo())
        {
            throw new BadRequestException(
                $"Consecutivo fuera de rango: {consecutivo} (máx {MaxConsecutivo()}).");
        }

        return $"{_qr.Prefix}{consecutivo.ToString().PadLeft(_qr.SequenceLength, '0')}";
    }

    public IReadOnlyList<string> FormatRange(int inicio, int fin)
    {
        if (fin < inicio)
        {
            throw new BadRequestException("El consecutivo final debe ser mayor o igual al inicial.");
        }

        var codes = new List<string>();
        for (var i = inicio; i <= fin; i++)
        {
            codes.Add(FormatCode(i));
        }

        return codes;
    }

    public int MaxConsecutivo()
    {
        if (_qr.SequenceLength >= 10)
        {
            return int.MaxValue;
        }

        return (int)Math.Pow(10, _qr.SequenceLength) - 1;
    }

    public bool IsValidFormat(string code)
    {
        var pattern = $"^{Regex.Escape(_qr.Prefix)}\\d{{{_qr.SequenceLength}}}$";
        return Regex.IsMatch(code, pattern);
    }
}
