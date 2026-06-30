import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router, RouterLink } from '@angular/router';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should have menu labels', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const app = fixture.nativeElement;
    const menuItems = app.querySelectorAll('ion-label');
    expect(menuItems.length).toEqual(4);
    expect(menuItems[0].textContent).toContain('Inicio');
    expect(menuItems[1].textContent).toContain('Elementos');
    expect(menuItems[2].textContent).toContain('Fornituras');
    expect(menuItems[3].textContent).toContain('Asignación');
  });

  it('should have urls', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const app = fixture.nativeElement;
    expect(app.querySelectorAll('ion-item').length).toEqual(4);
    const router = TestBed.inject(Router);
    const links = fixture.debugElement
      .queryAll(By.directive(RouterLink))
      .map((el) => el.injector.get(RouterLink));
    expect(links.length).toEqual(4);
    expect(router.serializeUrl(links[0].urlTree!)).toEqual('/inicio');
    expect(router.serializeUrl(links[1].urlTree!)).toEqual('/elementos');
    expect(router.serializeUrl(links[2].urlTree!)).toEqual('/fornituras');
    expect(router.serializeUrl(links[3].urlTree!)).toEqual('/asignacion');
  });
});
