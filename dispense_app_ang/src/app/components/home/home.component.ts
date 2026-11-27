import {HttpErrorResponse} from '@angular/common/http';
import {DatePipe} from '@angular/common';
import {Component, computed, effect, inject, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {AuthService} from '../../auth/auth.service';
import {Dossier} from '../../model/dossier';
import {ETAT_DOSSIER_LABELS, EtatDossier} from '../../model/etat-dossier';
import {DispenseService} from '../../services/dispense.service';

type DashboardFilter = 'all' | 'open' | 'closed';

@Component({
  selector: 'app-home',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  private readonly authService = inject(AuthService);
  private readonly dispenseService = inject(DispenseService);
  private readonly route = inject(ActivatedRoute);

  private pendingRequestCount = 0;
  private lastLoadedUsername: string | null = null;

  readonly authenticated = this.authService.getAuthenticationState();
  readonly appUser = this.authService.getAppUser();
  readonly appUserLoading = this.authService.getAppUserLoading();
  readonly appUserError = this.authService.getAppUserError();
  readonly activeDossier = signal<Dossier | null>(null);
  readonly userDossiers = signal<Dossier[]>([]);
  readonly activeFilter = signal<DashboardFilter>('all');
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly etatLabels = ETAT_DOSSIER_LABELS;

  readonly visibleDossiers = computed(() => {
    switch (this.activeFilter()) {
      case 'open':
        return this.userDossiers().filter(dossier => dossier.etat !== EtatDossier.CLOTURE);
      case 'closed':
        return this.userDossiers().filter(dossier => dossier.etat === EtatDossier.CLOTURE);
      default:
        return this.userDossiers();
    }
  });

  constructor() {
    this.route.queryParamMap.subscribe(params => {
      const filter = params.get('filter');
      if (filter === 'open' || filter === 'closed') {
        this.activeFilter.set(filter);
      } else {
        this.activeFilter.set('all');
      }
    });

    effect(() => {
      const isAuthenticated = this.authenticated();
      const appUser = this.appUser();

      // Sans utilisateur connecte + synchronise, on ne garde rien a l'ecran.
      if (!isAuthenticated || !appUser?.username) {
        this.resetDashboardState();
        return;
      }

      if (this.lastLoadedUsername === appUser.username) {
        return;
      }

      this.lastLoadedUsername = appUser.username;
      this.loadStudentDashboard();
    });
  }

  login() {
    this.authService.login();
  }

  getCurrentUsername() {
    return this.authService.getCurrentUsername();
  }

  hasOpenDossier(): boolean {
    return this.activeDossier() !== null;
  }

  getListTitle(): string {
    switch (this.activeFilter()) {
      case 'open':
        return 'Mes dossiers non clotures';
      case 'closed':
        return 'Mes dossiers clotures';
      default:
        return 'Mes dossiers';
    }
  }

  getEmptyListMessage(): string {
    switch (this.activeFilter()) {
      case 'open':
        return 'Aucun dossier non cloture.';
      case 'closed':
        return 'Aucun dossier cloture.';
      default:
        return 'Aucun dossier disponible.';
    }
  }

  private loadStudentDashboard() {
    this.errorMessage.set(null);

    this.startLoading();
    this.dispenseService.getUserDossiers().subscribe({
      next: dossiers => {
        this.userDossiers.set(dossiers);
        this.finishLoading();
      },
      error: error => {
        console.error(error);
        this.userDossiers.set([]);
        this.errorMessage.set('Impossible de charger la liste des dossiers.');
        this.finishLoading();
      }
    });

    this.startLoading();
    this.dispenseService.getActiveDossier().subscribe({
      next: dossier => {
        this.activeDossier.set(dossier);
        this.finishLoading();
      },
      error: (error: HttpErrorResponse) => {
        console.error(error);
        this.activeDossier.set(null);

        // 404 = aucun dossier actif, ce n'est pas une panne critique.
        if (error.status !== 404) {
          this.errorMessage.set('Impossible de charger le dossier actif.');
        }

        this.finishLoading();
      }
    });
  }

  private startLoading() {
    this.pendingRequestCount += 1;
    this.loading.set(true);
  }

  private finishLoading() {
    this.pendingRequestCount = Math.max(0, this.pendingRequestCount - 1);
    this.loading.set(this.pendingRequestCount > 0);
  }

  private resetDashboardState() {
    this.lastLoadedUsername = null;
    this.pendingRequestCount = 0;
    this.loading.set(false);
    this.errorMessage.set(null);
    this.userDossiers.set([]);
    this.activeDossier.set(null);
    this.activeFilter.set('all');
  }
}
