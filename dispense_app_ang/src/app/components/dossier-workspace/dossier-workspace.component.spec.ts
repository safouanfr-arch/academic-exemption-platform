import {convertToParamMap, provideRouter} from '@angular/router';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {of} from 'rxjs';
import {DossierWorkspaceComponent} from './dossier-workspace.component';
import {DispenseService} from '../../services/dispense.service';

describe('DossierWorkspaceComponent', () => {
  let component: DossierWorkspaceComponent;
  let fixture: ComponentFixture<DossierWorkspaceComponent>;

  it('should not load sub-resources when dossier is not accessible', async () => {
    let coursCalls = 0;
    let demandesCalls = 0;
    let documentsCalls = 0;
    let validationCalls = 0;
    let correspondancesCalls = 0;

    const dispenseServiceMock = {
      getSections: () => of([]),
      listerEtablissementsConnus: () => of([]),
      getUserDossiers: () => of([]),
      getCoursReussis: () => {
        coursCalls += 1;
        return of([]);
      },
      getDemandesUe: () => {
        demandesCalls += 1;
        return of([]);
      },
      getDocuments: () => {
        documentsCalls += 1;
        return of([]);
      },
      getDossierValidation: () => {
        validationCalls += 1;
        return of({complet: false, erreurs: [], avertissements: []});
      },
      listerCorrespondancesConnues: () => {
        correspondancesCalls += 1;
        return of([]);
      }
    };

    await TestBed.configureTestingModule({
      imports: [DossierWorkspaceComponent],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {paramMap: of(convertToParamMap({id: '1'}))}},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DossierWorkspaceComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();

    expect(coursCalls).toBe(0);
    expect(demandesCalls).toBe(0);
    expect(documentsCalls).toBe(0);
    expect(validationCalls).toBe(0);
    expect(correspondancesCalls).toBe(0);
    expect(component.errorMessage()).toBe('Dossier introuvable ou inaccessible.');
  });

  it('should prevent deleting a linked course from the workspace', async () => {
    let deleteCalls = 0;

    const dispenseServiceMock = {
      getSections: () => of([{code: 'INFO', nom: 'Informatique'}]),
      listerEtablissementsConnus: () => of([]),
      getUserDossiers: () => of([{
        id: 1,
        date: '2026-04-05',
        etat: 'DEMANDE_EN_COURS',
        objetDemande: 'Test dossier',
        user: {username: 'et1', email: 'et1@example.test', nom: 'Nom', prenom: 'Prenom'},
        section: {code: 'INFO', nom: 'Informatique'}
      }]),
      getCoursReussis: () => of([{
        id: 10,
        etablissementNom: 'EPHEC',
        code: 'INFO101',
        nom: 'Algo',
        ects: 5,
        note: null,
        prgm: null,
        lienProgramme: null
      }]),
      getDemandesUe: () => of([{
        id: 20,
        ueCode: 'IPAP',
        ueNom: 'Programmation',
        ueEcts: 8,
        commentaire: null,
        coursReussisIds: [10]
      }]),
      getUesBySection: () => of([]),
      getDocuments: () => of([]),
      getDossierValidation: () => of({complet: false, erreurs: [], avertissements: []}),
      listerCorrespondancesConnues: () => of([]),
      deleteCoursReussi: () => {
        deleteCalls += 1;
        return of(void 0);
      }
    };

    await TestBed.configureTestingModule({
      imports: [DossierWorkspaceComponent],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {paramMap: of(convertToParamMap({id: '1'}))}},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DossierWorkspaceComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();

    component.deleteCoursReussi(10);

    expect(deleteCalls).toBe(0);
    expect(component.errorMessage()).toBe('Delie d abord ce cours de toutes les demandes UE avant de le supprimer.');
  });

  it('should not propose a course already linked to another demande', async () => {
    const dispenseServiceMock = {
      getSections: () => of([{code: 'INFO', nom: 'Informatique'}]),
      listerEtablissementsConnus: () => of([]),
      getUserDossiers: () => of([{
        id: 1,
        date: '2026-04-05',
        etat: 'DEMANDE_EN_COURS',
        objetDemande: 'Test dossier',
        user: {username: 'et1', email: 'et1@example.test', nom: 'Nom', prenom: 'Prenom'},
        section: {code: 'INFO', nom: 'Informatique'}
      }]),
      getCoursReussis: () => of([
        {
          id: 10,
          etablissementNom: 'EPHEC',
          code: 'INFO101',
          nom: 'Algo',
          ects: 5,
          note: null,
          prgm: null,
          lienProgramme: null
        },
        {
          id: 11,
          etablissementNom: 'EPHEC',
          code: 'INFO102',
          nom: 'Web',
          ects: 5,
          note: null,
          prgm: null,
          lienProgramme: null
        }
      ]),
      getDemandesUe: () => of([
        {
          id: 20,
          ueCode: 'IPAP',
          ueNom: 'Programmation',
          ueEcts: 8,
          commentaire: null,
          coursReussisIds: [10]
        },
        {
          id: 21,
          ueCode: 'IPID',
          ueNom: 'Projet',
          ueEcts: 9,
          commentaire: null,
          coursReussisIds: []
        }
      ]),
      getUesBySection: () => of([]),
      getDocuments: () => of([]),
      getDossierValidation: () => of({complet: false, erreurs: [], avertissements: []}),
      listerCorrespondancesConnues: () => of([])
    };

    await TestBed.configureTestingModule({
      imports: [DossierWorkspaceComponent],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {paramMap: of(convertToParamMap({id: '1'}))}},
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DossierWorkspaceComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();

    const availableForSecondDemande = component.getUnlinkedCourses({
      id: 21,
      ueCode: 'IPID',
      ueNom: 'Projet',
      ueEcts: 9,
      commentaire: null,
      coursReussisIds: []
    });

    expect(availableForSecondDemande.map(cours => cours.id)).toEqual([11]);
  });
});
