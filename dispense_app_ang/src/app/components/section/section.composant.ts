import {Component, inject, OnInit} from '@angular/core';
import {MatIcon} from '@angular/material/icon';
import {MatDivider, MatList, MatListItem, MatNavList} from '@angular/material/list';
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from '@angular/material/sidenav';
import {Section} from '../../model/section';
import {UeDetail} from '../../model/ue-detail';
import {Ue} from '../../model/ue';
import {DispenseService} from '../../services/dispense.service';
import {EuDetailComponent} from '../eu-detail/eu-detail.component';

@Component({
  selector: 'app-section',
  imports: [
    MatList,
    MatListItem,
    MatSidenav,
    MatSidenavContainer,
    MatSidenavContent,
    EuDetailComponent,
    MatNavList,
    MatDivider,
    MatIcon,
  ],
  templateUrl: './section.composant.html',
  styleUrl: './section.composant.css',
})
export class SectionComposant implements OnInit {
  private readonly dispenseService = inject(DispenseService);

  sections: Section[] = [];
  activeSection?: Section;

  // Quand une section a deja ete ouverte, on garde ses UE en memoire.
  private readonly sectionUeCache = new Map<string, Ue[]>();
  sectionUes: Ue[] = [];
  selectedUeDetail?: UeDetail;
  loadingUeDetail = false;
  ueDetailErrorMessage = '';

  loadSectionUes(sectionCode: string) {
    this.selectedUeDetail = undefined;
    this.loadingUeDetail = false;
    this.ueDetailErrorMessage = '';

    if (this.sectionUeCache.has(sectionCode)) {
      this.sectionUes = this.sectionUeCache.get(sectionCode)!;
      return;
    }

    this.dispenseService.getUesBySection(sectionCode).subscribe({
      next: ues => {
        this.sectionUes = ues;
        this.sectionUeCache.set(sectionCode, ues);
      },
      error: error => console.error(error)
    });
  }

  showSection(section: Section): void {
    this.activeSection = section;
    this.selectedUeDetail = undefined;
    this.loadingUeDetail = false;
    this.ueDetailErrorMessage = '';
    this.loadSectionUes(section.code);
  }

  showUeDetail(ue: Ue): void {
    this.loadingUeDetail = true;
    this.ueDetailErrorMessage = '';

    this.dispenseService.getUeDetail(ue.code).subscribe({
      next: ueDetail => {
        this.selectedUeDetail = ueDetail;
        this.loadingUeDetail = false;
      },
      error: error => {
        console.error(error);
        this.selectedUeDetail = undefined;
        this.loadingUeDetail = false;
        this.ueDetailErrorMessage = 'Impossible de charger les details de cette UE.';
      }
    });
  }

  ngOnInit(): void {
    this.dispenseService.getSections().subscribe({
      next: sections => {
        this.sections = sections;

        // On ouvre la premiere section pour que l'etudiant voie tout de suite une liste d'UE.
        if (this.sections.length > 0) {
          this.showSection(this.sections[0]);
        }
      },
      error: error => console.error(error)
    });
  }
}
