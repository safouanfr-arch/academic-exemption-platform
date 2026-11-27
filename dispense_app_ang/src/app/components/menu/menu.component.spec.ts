import {signal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';

import {AuthService} from '../../auth/auth.service';
import {DispenseService} from '../../services/dispense.service';
import {MenuComponent} from './menu.component';

describe('MenuComponent', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;
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
    getDossierCounts: () => of({traite: 0, enCours: 0})
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenuComponent],
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: authServiceMock},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
