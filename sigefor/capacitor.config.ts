import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.ionic.starter',
  appName: 'sigefor',
  webDir: 'www',
  plugins: {
    // Fotos (017): el plugin de cámara pide permisos en tiempo de ejecución. En builds nativos,
    // los permisos de cámara/galería se declaran además en AndroidManifest.xml / Info.plist.
    // En web se usa fallback a selección de archivo si no hay cámara/permiso (FR-004).
    Camera: {},
  },
};

export default config;
