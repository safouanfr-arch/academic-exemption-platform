import {HttpErrorResponse} from '@angular/common/http';
import {DatePipe} from '@angular/common';
import {Component, computed, inject, signal} from '@angular/core';
import {MatButton} from '@angular/material/button';
import {MatCard, MatCardActions, MatCardContent, MatCardTitle} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {MatError, MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {Router} from '@angular/router';
import {DispenseService} from '../../services/dispense.service';

@Component({
  selector: 'app-dossier-create',
  imports: [
    MatCard,
    MatCardTitle,
    MatIcon,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatInput,
    MatError,
    MatCardActions,
    MatButton,
    DatePipe
  ],
  templateUrl: './dossier-create.html',
  styleUrl: './dossier-create.css',
})
export class DossierCreate {
  private readonly dispenseService = inject(DispenseService);
  private readonly router = inject(Router);

  readonly dateCreation = new Date();
  readonly objetDemande = signal('');
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly canCreateDossier = computed(() =>
    this.objetDemande().trim().length >= 10
  );

  updateRequestSubject(value: string) {
    this.objetDemande.set(value);
    this.errorMessage.set(null);
  }

  createDossier() {
    // On bloque si le texte est trop court ou si l'utilisateur a deja clique.
    if (!this.canCreateDossier() || this.saving()) {
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    this.dispenseService.createDossier(this.objetDemande().trim()).subscribe({
      next: dossier => {
        this.saving.set(false);
        this.router.navigate(['/dossier', dossier.id]);
      },
      error: error => {
        console.error(error);
        this.saving.set(false);
        this.errorMessage.set(this.readApiErrorMessage(error));
      }
    });
  }

  private readApiErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim().length > 0) {
        return error.error;
      }

      if (error.error && typeof error.error === 'object') {
        if (typeof error.error.objetDemande === 'string') {
          return error.error.objetDemande;
        }

        if (typeof error.error.message === 'string') {
          return error.error.message;
        }
      }
    }

    return 'Creation du dossier impossible.';
  }
}
