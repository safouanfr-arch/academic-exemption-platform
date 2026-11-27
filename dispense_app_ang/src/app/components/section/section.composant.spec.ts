import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of} from 'rxjs';

import {DispenseService} from '../../services/dispense.service';
import {SectionComposant} from './section.composant';

describe('SectionComposant', () => {
  let component: SectionComposant;
  let fixture: ComponentFixture<SectionComposant>;

  const dispenseServiceMock = {
    getSections: () => of([{code: 'INFO', nom: 'Informatique'}]),
    getUesBySection: () => of([])
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SectionComposant],
      providers: [
        {provide: DispenseService, useValue: dispenseServiceMock}
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SectionComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
