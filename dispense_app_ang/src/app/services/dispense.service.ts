import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {CorrespondanceConnue} from '../model/correspondance-connue';
import {CoursExterneReference} from '../model/cours-externe-reference';
import {CoursReussi} from '../model/cours-reussi';
import {CoursReussiRequest} from '../model/cours-reussi-request';
import {CreateDossier} from '../model/create-dossier';
import {DemandeUe} from '../model/demande-ue';
import {DemandeUeRequest} from '../model/demande-ue-request';
import {DocumentDossier} from '../model/document-dossier';
import {DocumentRequest} from '../model/document-request';
import {Dossier} from '../model/dossier';
import {EtablissementReference} from '../model/etablissement-reference';
import {NbDossier} from '../model/nb-dossier';
import {Section} from '../model/section';
import {SectionUpdate} from '../model/section-update';
import {TypeDocument} from '../model/type-document';
import {UeDetail} from '../model/ue-detail';
import {Ue} from '../model/ue';
import {User} from '../model/user';
import {ValidationResponse} from '../model/validation-response';

@Injectable({
  providedIn: 'root',
})
export class DispenseService {
  private readonly httpClient = inject(HttpClient);

  private readonly sectionListUrl = `${environment.resourceServer.url}/api/ue/sections`;
  private readonly ueListUrl = `${environment.resourceServer.url}/api/ue/liste`;
  private readonly ueDetailBaseUrl = `${environment.resourceServer.url}/api/ue/detail`;
  private readonly userProfileBaseUrl = `${environment.resourceServer.url}/api/user/profile`;
  private readonly dossierCountUrl = `${environment.resourceServer.url}/api/dossier/nb`;
  private readonly dossierCreationUrl = `${environment.resourceServer.url}/api/dossier/add`;
  private readonly activeDossierUrl = `${environment.resourceServer.url}/api/dossier/actif`;
  private readonly userDossiersUrl = `${environment.resourceServer.url}/api/dossier/liste`;
  private readonly dossierBaseUrl = `${environment.resourceServer.url}/api/dossier`;
  private readonly referenceBaseUrl = `${environment.resourceServer.url}/api/references`;

  // ==================== REFERENCES ====================

  listerEtablissementsConnus(): Observable<EtablissementReference[]> {
    return this.httpClient.get<EtablissementReference[]>(`${this.referenceBaseUrl}/etablissements`);
  }

  listerCoursConnusPourEtablissement(etablissementId: number): Observable<CoursExterneReference[]> {
    return this.httpClient.get<CoursExterneReference[]>(
      `${this.referenceBaseUrl}/etablissements/${etablissementId}/cours`
    );
  }

  // ==================== UE ====================

  getSections(): Observable<Section[]> {
    return this.httpClient.get<Section[]>(this.sectionListUrl);
  }

  getUesBySection(sectionCode: string): Observable<Ue[]> {
    const params = new HttpParams().set('section', sectionCode);
    return this.httpClient.get<Ue[]>(this.ueListUrl, {params});
  }

  getUeDetail(ueCode: string): Observable<UeDetail> {
    return this.httpClient.get<UeDetail>(`${this.ueDetailBaseUrl}/${ueCode}`);
  }

  // ==================== UTILISATEUR ====================

  getUserProfile(username: string): Observable<User> {
    return this.httpClient.get<User>(`${this.userProfileBaseUrl}/${username}`);
  }

  // ==================== DOSSIERS ====================

  getDossierCounts(): Observable<NbDossier> {
    return this.httpClient.get<NbDossier>(this.dossierCountUrl);
  }

  getActiveDossier(): Observable<Dossier> {
    return this.httpClient.get<Dossier>(this.activeDossierUrl);
  }

  getUserDossiers(): Observable<Dossier[]> {
    return this.httpClient.get<Dossier[]>(this.userDossiersUrl);
  }

  createDossier(objetDemande: string): Observable<Dossier> {
    const payload: CreateDossier = {objetDemande};
    return this.httpClient.post<Dossier>(this.dossierCreationUrl, payload);
  }

  updateDossierSection(dossierId: number, sectionCode: string): Observable<Dossier> {
    const payload: SectionUpdate = {sectionCode};
    return this.httpClient.put<Dossier>(`${this.dossierBaseUrl}/${dossierId}/section`, payload);
  }

  getDossierValidation(dossierId: number): Observable<ValidationResponse> {
    return this.httpClient.get<ValidationResponse>(`${this.dossierBaseUrl}/${dossierId}/validation`);
  }

  submitDossier(dossierId: number): Observable<Dossier> {
    return this.httpClient.post<Dossier>(`${this.dossierBaseUrl}/${dossierId}/soumettre`, {});
  }

  // ==================== COURS REUSSIS ====================

  getCoursReussis(dossierId: number): Observable<CoursReussi[]> {
    return this.httpClient.get<CoursReussi[]>(`${this.dossierBaseUrl}/${dossierId}/cours-reussis`);
  }

  addCoursReussi(dossierId: number, payload: CoursReussiRequest): Observable<CoursReussi> {
    return this.httpClient.post<CoursReussi>(`${this.dossierBaseUrl}/${dossierId}/cours-reussis`, payload);
  }

  deleteCoursReussi(dossierId: number, coursId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.dossierBaseUrl}/${dossierId}/cours-reussis/${coursId}`);
  }

  listerCorrespondancesConnues(dossierId: number): Observable<CorrespondanceConnue[]> {
    return this.httpClient.get<CorrespondanceConnue[]>(`${this.dossierBaseUrl}/${dossierId}/correspondances`);
  }

  synchroniserCorrespondancesConnues(dossierId: number): Observable<DemandeUe[]> {
    return this.httpClient.post<DemandeUe[]>(`${this.dossierBaseUrl}/${dossierId}/correspondances/sync`, {});
  }

  // ==================== DEMANDES UE ====================

  getDemandesUe(dossierId: number): Observable<DemandeUe[]> {
    return this.httpClient.get<DemandeUe[]>(`${this.dossierBaseUrl}/${dossierId}/demandes-ue`);
  }

  addDemandeUe(dossierId: number, payload: DemandeUeRequest): Observable<DemandeUe> {
    return this.httpClient.post<DemandeUe>(`${this.dossierBaseUrl}/${dossierId}/demandes-ue`, payload);
  }

  deleteDemandeUe(dossierId: number, demandeId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.dossierBaseUrl}/${dossierId}/demandes-ue/${demandeId}`);
  }

  linkCoursToDemandeUe(dossierId: number, demandeId: number, coursId: number): Observable<DemandeUe> {
    return this.httpClient.post<DemandeUe>(
      `${this.dossierBaseUrl}/${dossierId}/demandes-ue/${demandeId}/cours/${coursId}`,
      {}
    );
  }

  unlinkCoursFromDemandeUe(dossierId: number, demandeId: number, coursId: number): Observable<DemandeUe> {
    return this.httpClient.delete<DemandeUe>(
      `${this.dossierBaseUrl}/${dossierId}/demandes-ue/${demandeId}/cours/${coursId}`
    );
  }

  // ==================== DOCUMENTS ====================

  getDocuments(dossierId: number): Observable<DocumentDossier[]> {
    return this.httpClient.get<DocumentDossier[]>(`${this.dossierBaseUrl}/${dossierId}/documents`);
  }

  addDocument(dossierId: number, payload: DocumentRequest): Observable<DocumentDossier> {
    return this.httpClient.post<DocumentDossier>(`${this.dossierBaseUrl}/${dossierId}/documents`, payload);
  }

  uploadDocument(
    dossierId: number,
    typeDocument: TypeDocument,
    file: File,
    coursReussiId: number | null
  ): Observable<DocumentDossier> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('typeDocument', typeDocument);

    if (coursReussiId !== null) {
      formData.append('coursReussiId', String(coursReussiId));
    }

    return this.httpClient.post<DocumentDossier>(
      `${this.dossierBaseUrl}/${dossierId}/documents/upload`,
      formData
    );
  }

  deleteDocument(dossierId: number, documentId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.dossierBaseUrl}/${dossierId}/documents/${documentId}`);
  }
}
