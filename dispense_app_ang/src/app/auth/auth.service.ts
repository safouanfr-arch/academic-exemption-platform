import {effect, inject, Injectable, signal} from '@angular/core';
import {KEYCLOAK_EVENT_SIGNAL, KeycloakEventType, ReadyArgs, typeEventArgs} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {User} from '../model/user';
import {DispenseService} from '../services/dispense.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  readonly authenticatedState = signal(false);
  readonly appUserState = signal<User | null>(null);
  readonly appUserLoadingState = signal(false);
  readonly appUserErrorState = signal<string | null>(null);

  private currentUsername = '';
  private lastSyncedUsername: string | null = null;

  private readonly keycloakClient = inject(Keycloak);
  private readonly keycloakEventSignal = inject(KEYCLOAK_EVENT_SIGNAL);
  private readonly dispenseService = inject(DispenseService);

  constructor() {
    effect(() => {
      const authEvent = this.keycloakEventSignal();

      if (authEvent.type === KeycloakEventType.Ready) {
        this.handleKeycloakReady(authEvent);
      }

      if (authEvent.type === KeycloakEventType.AuthLogout) {
        this.resetAuthState();
      }
    });
  }

  private handleKeycloakReady(authEvent: {args?: unknown}) {
    const isConnected = typeEventArgs<ReadyArgs>(authEvent.args);
    this.authenticatedState.set(isConnected);

    // Si Keycloak n'a pas de session active, on vide aussi l'etat front local.
    if (!isConnected) {
      this.resetAuthState();
      return;
    }

    this.keycloakClient.loadUserProfile()
      .then(userProfile => {
        this.currentUsername = userProfile.username ?? '';

        if (!this.currentUsername) {
          this.appUserState.set(null);
          return;
        }

        // Ici on synchronise le "profil applicatif" local cote serveur.
        this.syncBackendUserProfile(this.currentUsername);
      })
      .catch(error => {
        console.error(error);
        this.resetAuthState();
      });
  }

  private syncBackendUserProfile(username: string) {
    // Si on a deja charge ce profil local, on evite un aller-retour HTTP inutile.
    if (this.lastSyncedUsername === username && this.appUserState()) {
      return;
    }

    this.appUserLoadingState.set(true);
    this.appUserErrorState.set(null);

    this.dispenseService.getUserProfile(username).subscribe({
      next: user => {
        this.appUserState.set(user);
        this.appUserLoadingState.set(false);
        this.lastSyncedUsername = username;
      },
      error: error => {
        console.error(error);
        this.appUserState.set(null);
        this.appUserLoadingState.set(false);
        this.appUserErrorState.set('Impossible de synchroniser le profil applicatif.');
        this.lastSyncedUsername = null;
      }
    });
  }

  private resetAuthState() {
    this.currentUsername = '';
    this.authenticatedState.set(false);
    this.appUserState.set(null);
    this.appUserLoadingState.set(false);
    this.appUserErrorState.set(null);
    this.lastSyncedUsername = null;
  }

  getAuthenticationState() {
    return this.authenticatedState;
  }

  getAppUser() {
    return this.appUserState;
  }

  getAppUserLoading() {
    return this.appUserLoadingState;
  }

  getAppUserError() {
    return this.appUserErrorState;
  }

  getCurrentUsername() {
    return this.currentUsername;
  }

  login() {
    this.keycloakClient.login();
  }

  logout() {
    this.keycloakClient.logout();
  }
}
