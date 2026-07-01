import { Injectable } from '@angular/core';
import { Preferences } from '@capacitor/preferences';
import { driver, Driver, DriveStep } from 'driver.js';
import { STORAGE_KEYS } from '../constants/storage-keys';

/**
 * Envoltorio del recorrido guiado (driver.js) aislado tras un servicio (DIP, ADR 0015): gestiona la
 * creación, arranque y destrucción/limpieza del tour, y la marca de "ya visto" por dispositivo en
 * Capacitor Preferences. Reutilizable por cualquier página; hoy lo usa el inicio (US4).
 */
@Injectable({ providedIn: 'root' })
export class TourService {
  private activeDriver: Driver | null = null;

  /** ¿El usuario/dispositivo ya vio el recorrido del inicio? */
  async hasSeenHomeTour(): Promise<boolean> {
    const { value } = await Preferences.get({ key: STORAGE_KEYS.tourHomeDone });
    return value === 'true';
  }

  private async markHomeTourSeen(): Promise<void> {
    await Preferences.set({ key: STORAGE_KEYS.tourHomeDone, value: 'true' });
  }

  /** Arranca el recorrido de forma automática solo la primera vez; queda marcado como visto. */
  async autoStartHomeTour(steps: DriveStep[]): Promise<void> {
    if (await this.hasSeenHomeTour()) {
      return;
    }
    await this.markHomeTourSeen();
    this.start(steps);
  }

  /** Relanza el recorrido a demanda ("Ver tutorial"), sin tocar la marca de primera vez. */
  startHomeTour(steps: DriveStep[]): void {
    this.start(steps);
  }

  private start(steps: DriveStep[]): void {
    this.destroy();
    if (steps.length === 0) {
      return;
    }
    this.activeDriver = driver({
      showProgress: true,
      nextBtnText: 'Siguiente',
      prevBtnText: 'Anterior',
      doneBtnText: 'Listo',
      steps,
      onDestroyed: () => {
        this.activeDriver = null;
      },
    });
    this.activeDriver.drive();
  }

  /** Limpia cualquier recorrido activo (p. ej. al salir de la página). */
  destroy(): void {
    if (this.activeDriver) {
      this.activeDriver.destroy();
      this.activeDriver = null;
    }
  }
}
