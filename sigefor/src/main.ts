import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  RouteReuseStrategy,
  provideRouter,
  withPreloading,
  PreloadAllModules,
} from '@angular/router';
import { provideAppInitializer, inject } from '@angular/core';
import { IonicRouteStrategy, provideIonicAngular } from '@ionic/angular/standalone';

import { routes } from './app/app.routes';
import { AppComponent } from './app/app.component';
import { authInterceptor } from './app/core/interceptors/auth.interceptor';
import { AuthService } from './app/core/services/auth.service';
import { WebBarcodeDetectorScanner } from './app/core/qr-scan/optical-scanner';
import { provideOpticalScanner } from './app/core/qr-scan/optical-scanner.provider';
import { CapacitorBarcodeScannerService } from './app/core/qr-scan/capacitor-barcode-scanner';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideIonicAngular(),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => inject(AuthService).restoreSession()),
    CapacitorBarcodeScannerService,
    WebBarcodeDetectorScanner,
    // Escaneo óptico: Capacitor barcode-scanner (019) con fallback BarcodeDetector web (ADR 0008).
    provideOpticalScanner(),
  ],
});
