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
import { OpticalScanner, WebBarcodeDetectorScanner } from './app/core/qr-scan/optical-scanner';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideIonicAngular(),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => inject(AuthService).restoreSession()),
    // Enlaza el puerto de escaneo óptico (ADR 0008) con su implementación web; sin esto, los
    // consumidores de <app-qr-scan> (asignación, fornituras...) fallan con NG0201.
    { provide: OpticalScanner, useExisting: WebBarcodeDetectorScanner },
  ],
});
