import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';

import {DispenseService} from '../../services/dispense.service';
import {DossierStat} from './dossier-stat';

describe('DossierStat', () => {
  let component: DossierStat;
  let fixture: ComponentFixture<DossierStat>;

  const dispenseServiceMock = {
    getDossierCounts: () => of({traite: 0, enCours: 0})
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DossierStat],
      providers: [
        provideRouter([]),
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(DossierStat);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
