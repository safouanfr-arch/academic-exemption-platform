import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {DispenseService} from './dispense.service';

describe('DispenseService', () => {
  let service: DispenseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DispenseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
