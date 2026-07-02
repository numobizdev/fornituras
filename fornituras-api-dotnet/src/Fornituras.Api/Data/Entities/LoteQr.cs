namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Lote de etiquetas QR con rango de consecutivos.
/// </summary>
public class LoteQr : BaseEntity
{
    public int ConsecutivoInicial { get; set; }

    public int ConsecutivoFinal { get; set; }

    public int Cantidad { get; set; }

    public string Descripcion { get; set; } = string.Empty;

    /// <summary>Tamaño total del cuadrado de corte (cm), sin contar la etiqueta legible.</summary>
    public decimal QrSizeCm { get; set; }

    /// <summary>Margen blanco interno por lado, dentro del cuadrado (<see cref="QrSizeCm"/>).</summary>
    public decimal PaddingCm { get; set; }

    public LabelPosition LabelPosition { get; set; }

    public bool MostrarBordes { get; set; } = true;
}
