import {Component, inject, OnInit, signal} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {MatBadge} from '@angular/material/badge';
import {MatButton} from '@angular/material/button';
import {NbDossier} from '../../model/nb-dossier';
import {DispenseService} from '../../services/dispense.service';

@Component({
  selector: 'app-dossier-stat',
  imports: [MatBadge, MatButton],
  templateUrl: './dossier-stat.html',
  styleUrl: './dossier-stat.css',
})
export class DossierStat implements OnInit {
  private readonly router = inject(Router);
  private readonly dispenseService = inject(DispenseService);

  readonly dossierCounts = signal<NbDossier>({
    traite: 0,
    enCours: 0
  });

  loadDossierCounts() {
    this.dispenseService.getDossierCounts().subscribe({
      next: counts => {
        this.dossierCounts.set(counts);
      },
      error: error => {
        console.error(error);
      }
    });
  }

  ngOnInit() {
    this.loadDossierCounts();

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.loadDossierCounts();
      }
    });
  }

  protected goToOpenDossiers() {
    this.router.navigate(['/home'], {queryParams: {filter: 'open'}});
  }

  protected goToClosedDossiers() {
    this.router.navigate(['/home'], {queryParams: {filter: 'closed'}});
  }
}
