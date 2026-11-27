import {signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';

import {AuthService} from './auth/auth.service';
import {AppComponent} from './app';
import {DispenseService} from './services/dispense.service';

describe('AppComponent', () => {
  const authenticated = signal(false);

  const authServiceMock = {
    getAuthenticationState: () => authenticated,
    getAppUser: () => signal(null),
    getAppUserLoading: () => signal(false),
    getAppUserError: () => signal(null),
    getCurrentUsername: () => 'et1',
    login: () => undefined,
    logout: () => undefined
  };

  const dispenseServiceMock = {
    getDossierCounts: () => of({traite: 0, enCours: 0}),
    getSections: () => of([]),
    getUesBySection: () => of([])
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: authServiceMock},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-sidebar-cmp')).toBeTruthy();
  });
});
