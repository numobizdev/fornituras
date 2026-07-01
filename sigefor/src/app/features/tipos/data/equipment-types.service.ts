import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { CATALOG_CODES, CatalogItemSummary, Page as CatalogPage } from '../../../core/catalog/catalog.model';
import { CatalogService } from '../../../core/catalog/catalog.service';
import {
  EquipmentTypeCreateRequest,
  EquipmentTypeDetail,
  EquipmentTypeSummary,
  Page,
  SizeCreateRequest,
  SizeSummary,
} from './equipment-type.model';

/**
 * Adaptador del catálogo genérico (ADR 0007) a la API histórica de tipos/tallas: el catálogo de
 * tipos de fornitura es `TIPO_FORNITURA` y las tallas son `TALLA` (colgando del tipo vía padre).
 * Mantiene la interfaz previa para no reescribir las páginas de tipos ni los formularios de fornitura.
 */
@Injectable({ providedIn: 'root' })
export class EquipmentTypesService {
  private readonly catalog = inject(CatalogService);

  list(options: { active?: boolean; page?: number; size?: number } = {}): Observable<Page<EquipmentTypeSummary>> {
    return this.catalog
      .listItems(CATALOG_CODES.TIPO_FORNITURA, options)
      .pipe(map((page) => this.mapTypePage(page)));
  }

  getById(id: number): Observable<EquipmentTypeDetail> {
    return forkJoin({
      type: this.catalog.getItem(id),
      sizes: this.catalog.listActiveItems(CATALOG_CODES.TALLA, id),
    }).pipe(map(({ type, sizes }) => this.toTypeDetail(type, sizes.map((s) => this.toSize(s)))));
  }

  create(request: EquipmentTypeCreateRequest): Observable<EquipmentTypeDetail> {
    return this.catalog
      .createItem(CATALOG_CODES.TIPO_FORNITURA, {
        nombre: request.nombre,
        descripcion: request.descripcion,
        fotoUrl: request.fotoUrl,
      })
      .pipe(map((item) => this.toTypeDetail(item, [])));
  }

  update(id: number, request: EquipmentTypeCreateRequest): Observable<EquipmentTypeDetail> {
    return this.catalog
      .updateItem(id, {
        nombre: request.nombre,
        descripcion: request.descripcion,
        fotoUrl: request.fotoUrl,
      })
      .pipe(map((item) => this.toTypeDetail(item, [])));
  }

  deactivate(id: number): Observable<void> {
    return this.catalog.deactivateItem(id);
  }

  listSizes(equipmentTypeId?: number): Observable<SizeSummary[]> {
    return this.catalog
      .listActiveItems(CATALOG_CODES.TALLA, equipmentTypeId ?? null)
      .pipe(map((items) => items.map((item) => this.toSize(item))));
  }

  createSize(request: SizeCreateRequest): Observable<SizeSummary> {
    return this.catalog
      .createItem(CATALOG_CODES.TALLA, {
        nombre: request.etiqueta,
        parentItemId: request.equipmentTypeId,
      })
      .pipe(map((item) => this.toSize(item)));
  }

  deactivateSize(id: number): Observable<void> {
    return this.catalog.deactivateItem(id);
  }

  private mapTypePage(page: CatalogPage<CatalogItemSummary>): Page<EquipmentTypeSummary> {
    return { ...page, content: page.content.map((item) => this.toTypeSummary(item)) };
  }

  private toTypeSummary(item: CatalogItemSummary): EquipmentTypeSummary {
    return {
      id: item.id,
      nombre: item.nombre,
      descripcion: item.descripcion,
      fotoUrl: item.fotoUrl,
      active: item.active,
    };
  }

  private toTypeDetail(item: CatalogItemSummary, sizes: SizeSummary[]): EquipmentTypeDetail {
    return { ...this.toTypeSummary(item), sizes, createdAt: '', updatedAt: '' };
  }

  private toSize(item: CatalogItemSummary): SizeSummary {
    return {
      id: item.id,
      etiqueta: item.nombre,
      equipmentTypeId: item.parentItemId,
      active: item.active,
    };
  }
}
