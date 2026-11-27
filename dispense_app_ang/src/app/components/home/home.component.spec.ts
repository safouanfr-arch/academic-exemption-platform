import {HttpErrorResponse} from '@angular/common/http';
import {signal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';

import {AuthService} from '../../auth/auth.service';
import {DispenseService} from '../../services/dispense.service';
import {HomeComponent} from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  const authenticated = signal(false);
  const appUser = signal(null);
  const appUserLoading = signal(false);
  const appUserError = signal<string | null>(null);

  const authServiceMock = {
    getAuthenticationState: () => authenticated,
    getAppUser: () => appUser,
    getAppUserLoading: () => appUserLoading,
    getAppUserError: () => appUserError,
    getCurrentUsername: () => 'et1'
  };

  const dispenseServiceMock = {
    getUserDossiers: () => of([]),
    getActiveDossier: () => throwError(() => new HttpErrorResponse({status: 404}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {queryParamMap: of(convertToParamMap({}))}},
        {provide: AuthService, useValue: authServiceMock},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
