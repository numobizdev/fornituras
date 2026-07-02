import { Injectable, inject } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';
import { ActionSheetButton, ActionSheetController } from '@ionic/angular/standalone';
import { Html5Qrcode } from 'html5-qrcode';
import { QrCaptureError } from './qr-scan.types';

const PREF_KEY = 'qr_scan_camera_device_id';

export interface CameraOption {
  deviceId: string;
  label: string;
}

/**
 * Enumeración y elección de cámara en web (escritorio con varias webcams).
 * En nativo no aplica: el plugin Capacitor gestiona la cámara del dispositivo.
 */
@Injectable({ providedIn: 'root' })
export class CameraSelectionService {
  private readonly actionSheetController = inject(ActionSheetController);

  isWebPlatform(): boolean {
    return Capacitor.getPlatform() === 'web';
  }

  /**
   * Resuelve el `deviceId` a usar antes de abrir el escáner en web.
   * @returns `null` si el usuario canceló; `undefined` en plataformas nativas.
   */
  async resolveDeviceIdForScan(): Promise<string | null | undefined> {
    if (!this.isWebPlatform()) {
      return undefined;
    }
    const cameras = await this.listCameras();
    if (cameras.length === 0) {
      throw this.error('no-camera', 'No se encontró ninguna cámara. Use el lector o teclee el código.');
    }
    if (cameras.length === 1) {
      await this.saveDeviceId(cameras[0].deviceId);
      return cameras[0].deviceId;
    }
    return this.pickCamera(cameras);
  }

  async listCameras(): Promise<CameraOption[]> {
    await this.ensureCameraPermission();
    const devices = await Html5Qrcode.getCameras();
    return devices.map((device, index) => ({
      deviceId: device.id,
      label: device.label?.trim() || `Cámara ${index + 1}`,
    }));
  }

  private async pickCamera(cameras: CameraOption[]): Promise<string | null> {
    const savedId = await this.getSavedDeviceId();
    const ordered = this.orderCameras(cameras, savedId);

    return new Promise<string | null>((resolve) => {
      let settled = false;
      const settle = (deviceId: string | null): void => {
        if (settled) {
          return;
        }
        settled = true;
        resolve(deviceId);
      };

      const buttons: ActionSheetButton[] = ordered.map((camera) => ({
        text: camera.deviceId === savedId ? `${camera.label} (última usada)` : camera.label,
        handler: () => {
          void this.saveDeviceId(camera.deviceId);
          settle(camera.deviceId);
        },
      }));
      buttons.push({
        text: 'Cancelar',
        role: 'cancel',
        handler: () => settle(null),
      });

      void this.actionSheetController
        .create({
          header: 'Seleccione la cámara',
          subHeader: 'Elija la webcam para escanear el código QR',
          buttons,
        })
        .then(async (sheet) => {
          await sheet.present();
          await sheet.onDidDismiss();
          settle(null);
        });
    });
  }

  private orderCameras(cameras: CameraOption[], savedId: string | null): CameraOption[] {
    if (!savedId) {
      return cameras;
    }
    const saved = cameras.find((camera) => camera.deviceId === savedId);
    if (!saved) {
      return cameras;
    }
    return [saved, ...cameras.filter((camera) => camera.deviceId !== savedId)];
  }

  private async getSavedDeviceId(): Promise<string | null> {
    const { value } = await Preferences.get({ key: PREF_KEY });
    return value ?? null;
  }

  private async saveDeviceId(deviceId: string): Promise<void> {
    await Preferences.set({ key: PREF_KEY, value: deviceId });
  }

  private async ensureCameraPermission(): Promise<void> {
    if (typeof navigator === 'undefined' || navigator.mediaDevices?.getUserMedia === undefined) {
      throw this.error('unsupported', 'El escaneo por cámara no está disponible en este dispositivo.');
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      stream.getTracks().forEach((track) => track.stop());
    } catch (cause) {
      throw this.fromGetUserMediaError(cause);
    }
  }

  private fromGetUserMediaError(cause: unknown): QrCaptureError {
    const name = cause instanceof DOMException ? cause.name : '';
    if (name === 'NotAllowedError' || name === 'SecurityError') {
      return this.error('permission-denied', 'Permiso de cámara denegado. Use el lector o teclee el código.');
    }
    if (name === 'NotFoundError' || name === 'OverconstrainedError') {
      return this.error('no-camera', 'No se encontró una cámara. Use el lector o teclee el código.');
    }
    return this.error('scan-failed', 'No se pudo acceder a la cámara. Use el lector o teclee el código.');
  }

  private error(reason: QrCaptureError['reason'], message: string): QrCaptureError {
    return { reason, message };
  }
}
