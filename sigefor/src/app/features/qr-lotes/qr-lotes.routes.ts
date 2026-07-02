import { Routes } from '@angular/router';

export const QR_LOTES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/qr-lotes-list/qr-lotes-list.page').then((m) => m.QrLotesListPage),
  },
  {
    path: 'generar',
    loadComponent: () =>
      import('./pages/qr-lote-generar/qr-lote-generar.page').then((m) => m.QrLoteGenerarPage),
  },
  {
    path: ':id/exito',
    loadComponent: () =>
      import('./pages/qr-lote-exito/qr-lote-exito.page').then((m) => m.QrLoteExitoPage),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/qr-lote-detail/qr-lote-detail.page').then((m) => m.QrLoteDetailPage),
  },
];
