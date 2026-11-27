import {signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {KEYCLOAK_EVENT_SIGNAL, KeycloakEventType} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {of} from 'rxjs';

import {DispenseService} from '../services/dispense.service';
import {AuthService} from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  const keycloakEventSignal = signal({
    type: KeycloakEventType.Ready,
    args: false
  });

  const keycloakMock = {
    loadUserProfile: () => Promise.resolve({username: 'et1'}),
    realmAccess: {roles: ['ETUDIANT']},
    login: () => Promise.resolve(),
    logout: () => Promise.resolve()
  };

  const dispenseServiceMock = {
    getUserProfile: () => of({
      username: 'et1',
      prenom: 'Nom',
      nom: 'Et1',
      email: 'et1@example.test'
    })
  };

  beforeEach(() => {
    keycloakEventSignal.set({
      type: KeycloakEventType.Ready,
      args: false
    });

    TestBed.configureTestingModule({
      providers: [
        {provide: Keycloak, useValue: keycloakMock},
        {provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEventSignal},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    });
  });

  it('should be created', () => {
    service = TestBed.inject(AuthService);
    expect(service).toBeTruthy();
  });
});
