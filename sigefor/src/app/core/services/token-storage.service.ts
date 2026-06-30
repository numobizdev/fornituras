import { Injectable } from '@angular/core';
import { Preferences } from '@capacitor/preferences';
import { STORAGE_KEYS } from '../constants/storage-keys';
import { StoredSession, UserSummary } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  async saveSession(session: StoredSession): Promise<void> {
    await Promise.all([
      Preferences.set({ key: STORAGE_KEYS.token, value: session.token }),
      Preferences.set({ key: STORAGE_KEYS.tokenType, value: session.tokenType }),
      Preferences.set({ key: STORAGE_KEYS.expiresAt, value: String(session.expiresAt) }),
      Preferences.set({ key: STORAGE_KEYS.user, value: JSON.stringify(session.user) }),
    ]);
  }

  async loadSession(): Promise<StoredSession | null> {
    const { value: token } = await Preferences.get({ key: STORAGE_KEYS.token });
    const { value: tokenType } = await Preferences.get({ key: STORAGE_KEYS.tokenType });
    const { value: expiresAt } = await Preferences.get({ key: STORAGE_KEYS.expiresAt });
    const { value: userJson } = await Preferences.get({ key: STORAGE_KEYS.user });

    if (!token || !tokenType || !expiresAt || !userJson) {
      return null;
    }

    return {
      token,
      tokenType,
      expiresAt: Number(expiresAt),
      user: JSON.parse(userJson) as UserSummary,
    };
  }

  async clearSession(): Promise<void> {
    await Promise.all([
      Preferences.remove({ key: STORAGE_KEYS.token }),
      Preferences.remove({ key: STORAGE_KEYS.tokenType }),
      Preferences.remove({ key: STORAGE_KEYS.expiresAt }),
      Preferences.remove({ key: STORAGE_KEYS.user }),
    ]);
  }
}
