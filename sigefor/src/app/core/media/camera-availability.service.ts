import { Injectable } from '@angular/core';
import { Camera } from '@capacitor/camera';

export type CameraAvailability = 'available' | 'unavailable' | 'denied';

/**
 * Detecta si la cámara web/nativa puede usarse para `@capacitor/camera` sin abrirla al cargar
 * el formulario (017 / FR-004).
 */
@Injectable({ providedIn: 'root' })
export class CameraAvailabilityService {
  isHardwareAvailable(): boolean {
    return typeof navigator !== 'undefined' && navigator.mediaDevices?.getUserMedia !== undefined;
  }

  async checkAvailability(): Promise<CameraAvailability> {
    if (!this.isHardwareAvailable()) {
      return 'unavailable';
    }
    try {
      const status = await Camera.checkPermissions();
      if (status.camera === 'denied') {
        return 'denied';
      }
      return 'available';
    } catch {
      return 'unavailable';
    }
  }

  messageFor(status: CameraAvailability): string | null {
    if (status === 'unavailable') {
      return 'Cámara no disponible en este dispositivo. Usa «Elegir archivo».';
    }
    if (status === 'denied') {
      return 'Permiso de cámara denegado. Usa «Elegir archivo».';
    }
    return null;
  }
}
