import { TestBed, inject } from '@angular/core/testing';

import { TreatmentApiService } from './treatment-api.service';

describe('TreatmentApiService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TreatmentApiService]
    });
  });

  it('should be created', inject([TreatmentApiService], (service: TreatmentApiService) => {
    expect(service).toBeTruthy();
  }));
});
