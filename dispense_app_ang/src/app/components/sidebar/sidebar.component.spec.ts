import {signal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';

import {AuthService} from '../../auth/auth.service';
import {DispenseService} from '../../services/dispense.service';
import {SidebarComponent} from './sidebar.component';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
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
      imports: [SidebarComponent],
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: authServiceMock},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
