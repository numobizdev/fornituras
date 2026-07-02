export type LabelPosition = 'NONE' | 'TOP' | 'BOTTOM';

export type QrExportFormat = 'PDF' | 'ZIP';

/** Alineado con el límite por defecto del backend (`QrOptions.MaxBatchSize`). */
export const QR_MAX_BATCH_SIZE = 10_000;

export interface GenerateQrRequest {
  descripcion: string;
  cantidad: number;
  qrSizeCm: number;
  paddingCm: number;
  labelPosition: LabelPosition;
  mostrarBordes: boolean;
}

export interface ReprintQrRequest {
  qrSizeCm: number;
  paddingCm: number;
  labelPosition: LabelPosition;
  mostrarBordes: boolean;
}

export interface LoteQrSummary {
  id: number;
  consecutivoInicial: number;
  consecutivoFinal: number;
  descripcion: string;
  cantidad: number;
  qrSizeCm: number;
  paddingCm: number;
  labelPosition: LabelPosition;
  mostrarBordes: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CodigoQrSummary {
  codigo: string;
  loteQrId: number;
}

export const LABEL_POSITION_OPTIONS: ReadonlyArray<{ value: LabelPosition; label: string }> = [
  { value: 'NONE', label: 'Solo QR (sin código)' },
  { value: 'TOP', label: 'Código arriba del QR' },
  { value: 'BOTTOM', label: 'Código abajo del QR' },
];

export const LABEL_POSITION_LABELS: Record<LabelPosition, string> = {
  NONE: 'Solo QR (sin código)',
  TOP: 'Código arriba del QR',
  BOTTOM: 'Código abajo del QR',
};

export function formatCodigoRange(inicio: number, fin: number): string {
  const pad = (n: number) => n.toString().padStart(6, '0');
  return `FOR-${pad(inicio)} – FOR-${pad(fin)}`;
}

export function defaultGenerateQrRequest(): GenerateQrRequest {
  return {
    descripcion: '',
    cantidad: 10,
    qrSizeCm: 3,
    paddingCm: 0.5,
    labelPosition: 'BOTTOM',
    mostrarBordes: true,
  };
}

export function reprintFromLote(lote: LoteQrSummary): ReprintQrRequest {
  return {
    qrSizeCm: lote.qrSizeCm,
    paddingCm: lote.paddingCm,
    labelPosition: lote.labelPosition,
    mostrarBordes: lote.mostrarBordes,
  };
}
