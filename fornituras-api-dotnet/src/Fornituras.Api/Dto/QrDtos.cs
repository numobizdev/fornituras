namespace Fornituras.Api.Dto;

public enum LabelPosition
{
    NONE,
    TOP,
    BOTTOM
}

public sealed record GenerateQrForm(
    string Descripcion,
    int Cantidad,
    decimal QrSizeCm,
    decimal PaddingCm,
    LabelPosition LabelPosition,
    bool MostrarBordes);

public sealed record LoteQrResponse(
    long Id,
    int ConsecutivoInicial,
    int ConsecutivoFinal,
    string Descripcion,
    int Cantidad,
    decimal QrSizeCm,
    decimal PaddingCm,
    LabelPosition LabelPosition,
    bool MostrarBordes,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public sealed record CodigoQrResponse(string Codigo, long LoteQrId);

public sealed record ReprintQrForm(
    decimal QrSizeCm,
    decimal PaddingCm,
    LabelPosition LabelPosition,
    bool MostrarBordes);
