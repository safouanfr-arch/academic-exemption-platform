import {HttpErrorResponse} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Observable, of, throwError} from 'rxjs';
import {DispenseService} from '../../services/dispense.service';

import {DossierCreate} from './dossier-create';

describe('DossierCreate', () => {
  let component: DossierCreate;
  let fixture: ComponentFixture<DossierCreate>;
  let createDossierResponse$: Observable<unknown>;
  let createDossierMock: (objetDemande: string) => Observable<unknown>;

  beforeEach(async () => {
    createDossierResponse$ = of({
      id: 1,
      date: '2026-04-05',
      etat: 'DEMANDE_EN_COURS',
      objetDemande: 'Demande de test',
      user: {username: 'et1', email: 'et1@example.test', nom: 'Nom', prenom: 'Prenom'},
      section: null
    });
    createDossierMock = () => createDossierResponse$;

    await TestBed.configureTestingModule({
      imports: [DossierCreate],
      providers: [
        provideRouter([]),
        {
          provide: DispenseService,
          useValue: {
            createDossier: createDossierMock
          }
        }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(DossierCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display backend dossier error when creation is refused', () => {
    createDossierResponse$ = throwError(() => new HttpErrorResponse({
      status: 400,
      error: {objetDemande: 'Il existe deja un dossier en cours'}
    }));

    component.updateRequestSubject('Demande assez detaillee');
    component.createDossier();

    expect(component.errorMessage()).toBe('Il existe deja un dossier en cours');
  });
});
