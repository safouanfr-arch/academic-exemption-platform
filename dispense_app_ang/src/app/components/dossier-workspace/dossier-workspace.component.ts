import {HttpErrorResponse} from '@angular/common/http';
import {DatePipe} from '@angular/common';
import {Component, inject, signal} from '@angular/core';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {forkJoin, map, Observable, of, switchMap} from 'rxjs';
import {CoursExterneReference} from '../../model/cours-externe-reference';
import {CorrespondanceConnue} from '../../model/correspondance-connue';
import {CoursReussi} from '../../model/cours-reussi';
import {CoursReussiRequest} from '../../model/cours-reussi-request';
import {DemandeUe} from '../../model/demande-ue';
import {DemandeUeRequest} from '../../model/demande-ue-request';
import {DocumentDossier} from '../../model/document-dossier';
import {DocumentRequest} from '../../model/document-request';
import {Dossier} from '../../model/dossier';
import {EtablissementReference} from '../../model/etablissement-reference';
import {ETAT_DOSSIER_LABELS, EtatDossier} from '../../model/etat-dossier';
import {Section} from '../../model/section';
import {TYPE_CORRESPONDANCE_LABELS} from '../../model/type-correspondance';
import {TypeDocument, TYPE_DOCUMENT_LABELS} from '../../model/type-document';
import {Ue} from '../../model/ue';
import {ValidationResponse} from '../../model/validation-response';
import {DispenseService} from '../../services/dispense.service';

@Component({
  selector: 'app-dossier-workspace',
  imports: [
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './dossier-workspace.component.html',
  styleUrl: './dossier-workspace.component.css'
})
export class DossierWorkspaceComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly dispenseService = inject(DispenseService);
  private readonly deleteLinkedCoursMessage = 'Delie d abord ce cours de toutes les demandes UE avant de le supprimer.';

  private dossierId: number | null = null;

  // Ces signals sont la "memoire vivante" de la page :
  // on y garde le dossier, ses cours, ses demandes, ses documents et les messages UI.
  readonly dossier = signal<Dossier | null>(null);
  readonly etablissements = signal<EtablissementReference[]>([]);
  readonly sections = signal<Section[]>([]);
  readonly uesSection = signal<Ue[]>([]);
  readonly coursConnus = signal<CoursExterneReference[]>([]);
  readonly coursReussis = signal<CoursReussi[]>([]);
  readonly demandes = signal<DemandeUe[]>([]);
  readonly documents = signal<DocumentDossier[]>([]);
  readonly validation = signal<ValidationResponse | null>(null);
  readonly correspondances = signal<CorrespondanceConnue[]>([]);
  readonly selectedSectionCode = signal('');
  readonly linkSelections = signal<Record<number, number | null>>({});
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly selectedEtablissementId = signal<number | null>(null);
  readonly selectedCoursConnuId = signal<number | null>(null);
  readonly manualEtablissementMode = signal(false);
  readonly manualCoursMode = signal(false);
  readonly documentMode = signal<'upload' | 'reference'>('upload');
  readonly selectedDocumentFile = signal<File | null>(null);
  readonly selectedCourseProgramFile = signal<File | null>(null);
  readonly etatLabels = ETAT_DOSSIER_LABELS;
  readonly typeDocumentLabels = TYPE_DOCUMENT_LABELS;
  readonly typeCorrespondanceLabels = TYPE_CORRESPONDANCE_LABELS;
  readonly typeDocumentOptions = Object.values(TypeDocument);

  readonly coursForm = new FormGroup({
    etablissementNom: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    code: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    nom: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    ects: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    note: new FormControl<number | null>(null, [Validators.required, Validators.min(10), Validators.max(20)]),
    lienProgramme: new FormControl('', {nonNullable: true}),
    ueCodeAssocie: new FormControl('', {nonNullable: true}),
  });

  readonly demandeForm = new FormGroup({
    ueCode: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    commentaire: new FormControl('', {nonNullable: true}),
  });

  readonly documentForm = new FormGroup({
    nomFichier: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    typeDocument: new FormControl<TypeDocument | null>(null, [Validators.required]),
    cheminFichier: new FormControl('', {nonNullable: true}),
    urlDocument: new FormControl('', {nonNullable: true}),
    coursReussiId: new FormControl<number | null>(null),
  });

  constructor() {
    this.loadKnownSchools();
    this.loadSections();
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!Number.isFinite(id) || id <= 0) {
        this.loading.set(false);
        this.errorMessage.set('Identifiant de dossier invalide.');
        return;
      }

      this.dossierId = id;
      this.loadWorkspaceData();
    });
  }

  isEditable(): boolean {
    const etat = this.dossier()?.etat;
    return etat === EtatDossier.DEMANDE_EN_COURS || etat === EtatDossier.ATTENTE_COMPLEMENT;
  }

  isSubmitState(): boolean {
    const etat = this.dossier()?.etat;
    return etat === EtatDossier.DEMANDE_EN_COURS || etat === EtatDossier.ATTENTE_COMPLEMENT;
  }

  hasPendingSectionChange(): boolean {
    const currentSectionCode = this.dossier()?.section?.code ?? '';
    return this.selectedSectionCode() !== currentSectionCode;
  }

  canCreateDemande(): boolean {
    return this.isEditable()
      && !!this.dossier()?.section
      && !this.hasPendingSectionChange()
      && this.getRequestableUes().length > 0;
  }

  canManageDocuments(): boolean {
    return this.isEditable();
  }

  peutSynchroniserCorrespondances(): boolean {
    return this.isEditable() && !!this.dossier()?.section;
  }

  canSubmit(): boolean {
    return this.isSubmitState() && !!this.validation()?.complet && !this.saving();
  }

  hasSavedSection(): boolean {
    return !!this.dossier()?.section;
  }

  hasCoursReussis(): boolean {
    return this.coursReussis().length > 0;
  }

  hasDemandes(): boolean {
    return this.demandes().length > 0;
  }

  hasDocuments(): boolean {
    return this.documents().length > 0;
  }

  hasDocumentType(type: TypeDocument): boolean {
    return this.documents().some(document => document.typeDocument === type);
  }

  hasLettreMotivation(): boolean {
    return this.hasDocumentType(TypeDocument.LETTRE_MOTIVATION);
  }

  getCoursJustificatifs(): CoursReussi[] {
    const idsCoursJustificatifs = new Set(this.demandes().flatMap(demande => demande.coursReussisIds));
    return this.coursReussis().filter(cours => idsCoursJustificatifs.has(cours.id));
  }

  hasBulletinGlobal(): boolean {
    return this.documents().some(document =>
      document.typeDocument === TypeDocument.BULLETIN && document.coursReussiId === null
    );
  }

  hasBulletinPourCours(coursId: number): boolean {
    return this.hasBulletinGlobal()
      || this.documents().some(document =>
        document.typeDocument === TypeDocument.BULLETIN && document.coursReussiId === coursId
      );
  }

  hasBulletinsPourCoursJustificatifs(): boolean {
    const coursJustificatifs = this.getCoursJustificatifs();
    return coursJustificatifs.length > 0
      && coursJustificatifs.every(cours => this.hasBulletinPourCours(cours.id));
  }

  canShowCorrespondanceStep(): boolean {
    return this.hasSavedSection() && !this.hasPendingSectionChange() && this.hasCoursReussis();
  }

  getSubmitLabel(): string {
    return this.dossier()?.etat === EtatDossier.ATTENTE_COMPLEMENT ? 'Resoumettre le dossier' : 'Soumettre le dossier';
  }

  shouldExpandSectionStep(): boolean {
    return this.isEditable() && (!this.hasSavedSection() || this.hasPendingSectionChange());
  }

  shouldExpandCoursStep(): boolean {
    return this.isEditable() && this.hasSavedSection() && !this.hasPendingSectionChange() && !this.hasCoursReussis();
  }

  shouldExpandCorrespondanceStep(): boolean {
    return this.isEditable() && this.canShowCorrespondanceStep() && this.getSuggestedUeCount() > 0 && !this.hasDemandes();
  }

  shouldExpandDemandesStep(): boolean {
    return this.isEditable() && this.hasCoursReussis() && !this.hasDocuments();
  }

  shouldExpandDocumentsStep(): boolean {
    return this.isEditable() && this.hasDemandes() && !this.hasDocuments();
  }

  shouldExpandValidationStep(): boolean {
    return !this.isEditable() || this.hasDocuments() || !!this.validation()?.erreurs.length;
  }

  getSubmissionReadinessLabel(isReady: boolean): string {
    return isReady ? 'Pret' : 'A completer';
  }

  getNextStepMessage(): string {
    if (!this.isEditable()) {
      return 'Le dossier est maintenant en lecture seule. Tu peux encore le consulter, mais plus le modifier.';
    }

    if (!this.hasSavedSection() || this.hasPendingSectionChange()) {
      return 'Etape 1 : choisis et enregistre la section de reference de ton dossier.';
    }

    if (!this.hasCoursReussis()) {
      return 'Etape 2 : ajoute tes cours reussis pour que l application puisse detecter les dispenses connues.';
    }

    if (!this.hasDemandes()) {
      return 'Etape 3 : applique les correspondances detectees ou ajoute manuellement les demandes UE restantes.';
    }

    if (!this.hasDocuments()) {
      return 'Etape 4 : ajoute les justificatifs obligatoires, comme la lettre de motivation et le bulletin.';
    }

    if (!this.validation()?.complet) {
      return 'Etape 5 : corrige les erreurs bloquantes puis relance la validation du dossier.';
    }

    return 'Ton dossier semble pret. Tu peux maintenant le soumettre.';
  }

  getStatusMessage(): string {
    const etat = this.dossier()?.etat;

    switch (etat) {
      case EtatDossier.DEMANDE_EN_COURS:
        return 'Le dossier est editable. Tu peux encore modifier, ajouter des references documentaires et le soumettre.';
      case EtatDossier.ATTENTE_COMPLEMENT:
        return 'Le dossier attend un complement. Tu peux le corriger puis le resoumettre.';
      case EtatDossier.TRAITEMENT_DIRECTION:
        return 'Le dossier est en traitement direction. Il est consultable mais non modifiable.';
      case EtatDossier.TRAITEMENT_PROFESSEUR:
        return 'Le dossier est en traitement professeur. Il est consultable mais non modifiable.';
      case EtatDossier.TRAITE:
        return 'Le dossier a ete traite. Il reste consultable uniquement.';
      case EtatDossier.CLOTURE:
        return 'Le dossier est cloture. Il reste consultable uniquement.';
      default:
        return 'Statut du dossier indisponible.';
    }
  }

  isGuidedEtablissementMode(): boolean {
    return !this.manualEtablissementMode();
  }

  isGuidedCourseMode(): boolean {
    return this.isGuidedEtablissementMode() && !this.manualCoursMode();
  }

  getSelectedEtablissementName(): string {
    const id = this.selectedEtablissementId();
    if (id === null) {
      return '';
    }

    return this.etablissements().find(item => item.id === id)?.nom ?? '';
  }

  getSelectedKnownCourse(): CoursExterneReference | null {
    const selectedId = this.selectedCoursConnuId();
    if (selectedId === null) {
      return null;
    }

    return this.coursConnus().find(item => item.id === selectedId) ?? null;
  }

  getSelectedDocumentFileName(): string {
    return this.selectedDocumentFile()?.name ?? '';
  }

  getSelectedCourseProgramFileName(): string {
    return this.selectedCourseProgramFile()?.name ?? '';
  }

  getLinkedCourses(demande: DemandeUe): CoursReussi[] {
    const linkedIds = new Set(demande.coursReussisIds);
    return this.coursReussis().filter(cours => linkedIds.has(cours.id));
  }

  hasLinkedCourses(demande: DemandeUe): boolean {
    return this.getLinkedCourses(demande).length > 0;
  }

  getDemandeReadinessLabel(demande: DemandeUe): string {
    return this.hasLinkedCourses(demande) ? 'Justifiee' : 'A completer';
  }

  getDemandeReadinessHint(demande: DemandeUe): string {
    if (this.hasLinkedCourses(demande)) {
      return 'Cette UE a deja au moins un cours justificatif lie.';
    }

    return 'Ajoute ou lie au moins un cours reussi pour justifier cette demande.';
  }

  getGloballyUnlinkedCourses(): CoursReussi[] {
    const globallyLinkedIds = new Set(this.demandes().flatMap(item => item.coursReussisIds));
    return this.coursReussis().filter(cours => !globallyLinkedIds.has(cours.id));
  }

  getUnlinkedCourses(_demande: DemandeUe): CoursReussi[] {
    return this.getGloballyUnlinkedCourses();
  }

  getRequestableUes(): Ue[] {
    const requestedCodes = new Set(this.demandes().map(demande => demande.ueCode));
    return this.uesSection().filter(ue => !requestedCodes.has(ue.code));
  }

  getAssociableUes(): Ue[] {
    return this.uesSection();
  }

  getRemainingUeCount(): number {
    return this.getRequestableUes().length;
  }

  getUnlinkedCoursesCount(): number {
    return this.getGloballyUnlinkedCourses().length;
  }

  getSuggestedUeCount(): number {
    return new Set(this.correspondances().map(item => item.ueCode)).size;
  }

  isCoursLinked(coursId: number): boolean {
    return this.demandes().some(demande => demande.coursReussisIds.includes(coursId));
  }

  getSelectedLinkCourseId(demandeId: number): number | null {
    return this.linkSelections()[demandeId] ?? null;
  }

  setSelectedSectionCode(sectionCode: string) {
    this.selectedSectionCode.set(sectionCode);
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }

  useGuidedEtablissementMode() {
    this.manualEtablissementMode.set(false);
    this.manualCoursMode.set(false);
    this.coursForm.patchValue({etablissementNom: ''});
    this.selectedCoursConnuId.set(null);
    this.coursConnus.set([]);
    this.resetCourseFormFields();
  }

  useManualEtablissementMode() {
    this.manualEtablissementMode.set(true);
    this.manualCoursMode.set(true);
    this.selectedEtablissementId.set(null);
    this.selectedCoursConnuId.set(null);
    this.coursConnus.set([]);
    this.coursForm.patchValue({etablissementNom: ''});
    this.resetCourseFormFields();
  }

  setSelectedEtablissementId(etablissementId: number | null) {
    this.selectedEtablissementId.set(etablissementId);
    this.selectedCoursConnuId.set(null);
    this.manualCoursMode.set(false);
    this.coursConnus.set([]);
    this.resetCourseFormFields();

    const etablissementNom = etablissementId === null ? '' : this.getSelectedEtablissementName();
    this.coursForm.patchValue({etablissementNom});

    if (etablissementId === null) {
      return;
    }

    this.loadKnownCoursesForSchool(etablissementId);
  }

  useGuidedCourseMode() {
    this.manualCoursMode.set(false);
    this.selectedCoursConnuId.set(null);
    this.resetCourseFormFields();
  }

  useManualCourseMode() {
    this.manualCoursMode.set(true);
    this.selectedCoursConnuId.set(null);
    this.resetCourseFormFields();
  }

  setSelectedCoursConnuId(coursId: number | null) {
    this.selectedCoursConnuId.set(coursId);
    const cours = this.getSelectedKnownCourse();

    if (!cours) {
      this.resetCourseFormFields();
      return;
    }

    this.coursForm.patchValue({
      code: cours.code,
      nom: cours.nom,
      ects: cours.ects,
      lienProgramme: cours.lienProgramme ?? ''
    });
  }

  setDocumentMode(mode: 'upload' | 'reference') {
    this.documentMode.set(mode);
    if (mode === 'upload') {
      this.documentForm.patchValue({cheminFichier: '', urlDocument: ''});
    } else {
      this.selectedDocumentFile.set(null);
    }
  }

  onDocumentFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.selectedDocumentFile.set(file);
    if (file) {
      this.documentForm.patchValue({nomFichier: file.name});
    }
  }

  onCourseProgramFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.selectedCourseProgramFile.set(file);
  }

  setLinkSelection(demandeId: number, coursId: number | null) {
    this.linkSelections.update(current => ({
      ...current,
      [demandeId]: coursId
    }));
  }

  getCoursLabelById(coursId: number | null): string {
    if (!coursId) {
      return 'Aucun cours associe';
    }

    const cours = this.coursReussis().find(item => item.id === coursId);
    return cours ? `${cours.code} - ${cours.nom}` : `Cours #${coursId}`;
  }

  saveSection() {
    if (!this.isEditable() || this.dossierId === null || !this.selectedSectionCode()) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.updateDossierSection(this.dossierId, this.selectedSectionCode()),
      'Section mise a jour.'
    );
  }

  addCoursReussi() {
    if (!this.isEditable() || this.dossierId === null) {
      return;
    }

    if (this.isGuidedEtablissementMode() && this.selectedEtablissementId() === null) {
      this.errorMessage.set('Choisis un etablissement dans la liste ou passe en saisie libre.');
      return;
    }

    if (this.isGuidedCourseMode() && this.selectedCoursConnuId() === null) {
      this.errorMessage.set('Choisis un cours connu ou passe en saisie libre.');
      return;
    }

    if (this.coursForm.invalid) {
      this.coursForm.markAllAsTouched();
      return;
    }

    const raw = this.coursForm.getRawValue();
    const isManualCourse = this.manualCoursMode();
    const selectedProgrammeFile = this.selectedCourseProgramFile();
    const ueCodeAssocie = raw.ueCodeAssocie.trim();

    if (isManualCourse && !this.hasSavedSection()) {
      this.errorMessage.set('Choisis d abord une section avant d encoder un cours inconnu et son UE associee.');
      return;
    }

    if (isManualCourse && !ueCodeAssocie) {
      this.errorMessage.set('Pour un cours inconnu, choisis l UE ISFCE associee.');
      return;
    }

    if (isManualCourse && !raw.lienProgramme.trim() && !selectedProgrammeFile) {
      this.errorMessage.set('Pour un cours inconnu, ajoute une URL de programme ou televerse le PDF du programme.');
      return;
    }

    const etablissementNom = this.manualEtablissementMode()
      ? raw.etablissementNom.trim()
      : this.getSelectedEtablissementName();
    const payload: CoursReussiRequest = {
      etablissementId: this.manualEtablissementMode() ? null : this.selectedEtablissementId(),
      coursExterneId: this.isGuidedCourseMode() ? this.selectedCoursConnuId() : null,
      etablissementNom,
      code: isManualCourse ? raw.code.trim() : null,
      nom: isManualCourse ? raw.nom.trim() : null,
      ects: isManualCourse ? Number(raw.ects) : null,
      note: raw.note !== null ? String(raw.note) : null,
      prgm: null,
      lienProgramme: isManualCourse ? raw.lienProgramme.trim() || null : null,
      hasPedagogicalUpload: isManualCourse && selectedProgrammeFile !== null
    };

    // Cas guide : on ajoute juste le cours connu.
    // Cas libre : on ajoute le cours, puis on cree ou reutilise la demande UE, puis on lie le cours.
    const request$ = this.dispenseService.addCoursReussi(this.dossierId, payload).pipe(
      switchMap(cours => {
        if (!isManualCourse || !ueCodeAssocie) {
          return of(cours);
        }

        const demandeExistante = this.demandes().find(demande => demande.ueCode === ueCodeAssocie);
        const demande$ = demandeExistante
          ? of(demandeExistante)
          : this.dispenseService.addDemandeUe(this.dossierId!, {ueCode: ueCodeAssocie, commentaire: null});

        return demande$.pipe(
          switchMap(demande => this.dispenseService.linkCoursToDemandeUe(this.dossierId!, demande.id, cours.id)),
          map(() => cours)
        );
      }),
      switchMap(cours => {
        if (!isManualCourse || !selectedProgrammeFile) {
          return of(cours);
        }

        return this.dispenseService
          .uploadDocument(this.dossierId!, TypeDocument.DOSSIER_PEDAGOGIQUE, selectedProgrammeFile, cours.id)
          .pipe(map(() => cours));
      })
    );

    this.runRequestAndReload(
      request$,
      isManualCourse ? 'Cours inconnu ajoute et relie a l UE choisie.' : 'Cours reussi ajoute.',
      () => {
        this.selectedCoursConnuId.set(null);
        this.coursConnus.set([]);
        this.selectedEtablissementId.set(null);
        this.manualEtablissementMode.set(false);
        this.manualCoursMode.set(false);
        this.selectedCourseProgramFile.set(null);
        this.coursForm.reset({
          etablissementNom: '',
          code: '',
          nom: '',
          ects: null,
          note: null,
          lienProgramme: '',
          ueCodeAssocie: ''
        });
      }
    );
  }

  deleteCoursReussi(coursId: number) {
    if (!this.isEditable() || this.dossierId === null) {
      return;
    }

    if (this.isCoursLinked(coursId)) {
      this.successMessage.set(null);
      this.errorMessage.set(this.deleteLinkedCoursMessage);
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.deleteCoursReussi(this.dossierId, coursId),
      'Cours reussi supprime.'
    );
  }

  addDemandeUe() {
    if (!this.canCreateDemande() || this.dossierId === null || this.demandeForm.invalid) {
      this.demandeForm.markAllAsTouched();
      return;
    }

    const raw = this.demandeForm.getRawValue();
    const payload: DemandeUeRequest = {
      ueCode: raw.ueCode,
      commentaire: raw.commentaire.trim() || null
    };

    this.runRequestAndReload(
      this.dispenseService.addDemandeUe(this.dossierId, payload),
      'Demande UE ajoutee.',
      () => {
        this.demandeForm.reset({
          ueCode: '',
          commentaire: ''
        });
      }
    );
  }

  deleteDemandeUe(demandeId: number) {
    if (!this.isEditable() || this.dossierId === null) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.deleteDemandeUe(this.dossierId, demandeId),
      'Demande UE supprimee.'
    );
  }

  linkCoursToDemande(demandeId: number) {
    if (!this.isEditable() || this.dossierId === null) {
      return;
    }

    const coursId = this.getSelectedLinkCourseId(demandeId);
    if (!coursId) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.linkCoursToDemandeUe(this.dossierId, demandeId, coursId),
      'Cours lie a la demande UE.',
      () => this.setLinkSelection(demandeId, null)
    );
  }

  unlinkCoursFromDemande(demandeId: number, coursId: number) {
    if (!this.isEditable() || this.dossierId === null) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.unlinkCoursFromDemandeUe(this.dossierId, demandeId, coursId),
      'Cours delie de la demande UE.'
    );
  }

  addDocument() {
    if (!this.canManageDocuments() || this.dossierId === null) {
      return;
    }

    const raw = this.documentForm.getRawValue();

    if (raw.typeDocument === null) {
      this.documentForm.markAllAsTouched();
      return;
    }

    if (this.documentMode() === 'upload') {
      const file = this.selectedDocumentFile();
      if (!file) {
        this.errorMessage.set('Selectionne un fichier a televerser.');
        return;
      }

      this.runRequestAndReload(
        this.dispenseService.uploadDocument(this.dossierId, raw.typeDocument, file, raw.coursReussiId ?? null),
        'Fichier justificatif ajoute.',
        () => {
          this.selectedDocumentFile.set(null);
          this.documentForm.reset({
            nomFichier: '',
            typeDocument: null,
            cheminFichier: '',
            urlDocument: '',
            coursReussiId: null
          });
        }
      );
      return;
    }

    if (this.documentForm.invalid) {
      this.documentForm.markAllAsTouched();
      return;
    }

    const payload: DocumentRequest = {
      nomFichier: raw.nomFichier.trim(),
      typeDocument: raw.typeDocument,
      cheminFichier: raw.cheminFichier.trim() || null,
      urlDocument: raw.urlDocument.trim() || null,
      coursReussiId: raw.coursReussiId ?? null
    };

    this.runRequestAndReload(
      this.dispenseService.addDocument(this.dossierId, payload),
      'Reference documentaire ajoutee.',
      () => {
      this.documentForm.reset({
          nomFichier: '',
          typeDocument: null,
          cheminFichier: '',
          urlDocument: '',
          coursReussiId: null
        });
      }
    );
  }

  deleteDocument(documentId: number) {
    if (!this.canManageDocuments() || this.dossierId === null) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.deleteDocument(this.dossierId, documentId),
      'Reference documentaire supprimee.'
    );
  }

  refreshValidation() {
    this.loadWorkspaceData('Validation mise a jour.');
  }

  submitDossier() {
    if (!this.canSubmit() || this.dossierId === null) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.submitDossier(this.dossierId),
      this.dossier()?.etat === EtatDossier.ATTENTE_COMPLEMENT ? 'Dossier resoumis.' : 'Dossier soumis.'
    );
  }

  appliquerCorrespondancesConnues() {
    if (!this.peutSynchroniserCorrespondances() || this.dossierId === null) {
      return;
    }

    this.runRequestAndReload(
      this.dispenseService.synchroniserCorrespondancesConnues(this.dossierId),
      'Correspondances connues synchronisees.'
    );
  }

  private loadSections() {
    this.dispenseService.getSections().subscribe({
      next: sections => {
        this.sections.set(sections);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Impossible de charger les sections.');
      }
    });
  }

  private loadKnownSchools() {
    this.dispenseService.listerEtablissementsConnus().subscribe({
      next: etablissements => {
        this.etablissements.set(etablissements);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Impossible de charger les etablissements de reference.');
      }
    });
  }

  private loadWorkspaceData(successMessage?: string) {
    if (this.dossierId === null) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.dispenseService.getUserDossiers().subscribe({
      next: dossiers => {
        const dossier = dossiers.find(item => item.id === this.dossierId) ?? null;
        if (!dossier) {
          this.dossier.set(null);
          this.coursReussis.set([]);
          this.demandes.set([]);
          this.documents.set([]);
          this.validation.set(null);
          this.correspondances.set([]);
          this.uesSection.set([]);
          this.loading.set(false);
          this.saving.set(false);
          this.errorMessage.set('Dossier introuvable ou inaccessible.');
          return;
        }

        this.dossier.set(dossier);
        this.selectedSectionCode.set(dossier.section?.code ?? '');
        this.linkSelections.set({});

        forkJoin({
          coursReussis: this.dispenseService.getCoursReussis(this.dossierId!),
          demandes: this.dispenseService.getDemandesUe(this.dossierId!),
          documents: this.dispenseService.getDocuments(this.dossierId!),
          validation: this.dispenseService.getDossierValidation(this.dossierId!),
          correspondances: this.dispenseService.listerCorrespondancesConnues(this.dossierId!)
        }).subscribe({
          next: ({coursReussis, demandes, documents, validation, correspondances}) => {
            this.coursReussis.set(coursReussis);
            this.demandes.set(demandes);
            this.documents.set(documents);
            this.validation.set(validation);
            this.correspondances.set(correspondances);
            this.loadAvailableUesForSavedSection(dossier.section?.code ?? null);

            this.loading.set(false);
            this.saving.set(false);
            if (successMessage) {
              this.successMessage.set(successMessage);
            }
          },
          error: error => {
            console.error(error);
            this.loading.set(false);
            this.saving.set(false);
            this.errorMessage.set(this.readApiErrorMessage(error, 'Impossible de charger le dossier.'));
          }
        });
      },
      error: error => {
        console.error(error);
        this.loading.set(false);
        this.saving.set(false);
        this.errorMessage.set(this.readApiErrorMessage(error, 'Impossible de charger le dossier.'));
      }
    });
  }

  private loadAvailableUesForSavedSection(sectionCode: string | null) {
    if (!sectionCode) {
      this.uesSection.set([]);
      this.demandeForm.patchValue({ueCode: ''});
      return;
    }

    this.dispenseService.getUesBySection(sectionCode).subscribe({
      next: ues => {
        this.uesSection.set(ues);
        const currentUeCode = this.demandeForm.getRawValue().ueCode;
        const hasCurrentValue = ues.some(ue => ue.code === currentUeCode);
        if (!hasCurrentValue) {
          this.demandeForm.patchValue({ueCode: ''});
        }
      },
      error: error => {
        console.error(error);
        this.uesSection.set([]);
        this.errorMessage.set('Impossible de charger les UE de la section.');
      }
    });
  }

  private loadKnownCoursesForSchool(etablissementId: number) {
    this.dispenseService.listerCoursConnusPourEtablissement(etablissementId).subscribe({
      next: cours => {
        this.coursConnus.set(cours);
        if (cours.length === 0) {
          this.manualCoursMode.set(true);
        }
      },
      error: error => {
        console.error(error);
        this.coursConnus.set([]);
        this.manualCoursMode.set(true);
        this.errorMessage.set('Impossible de charger les cours connus pour cet etablissement.');
      }
    });
  }

  private resetCourseFormFields() {
    this.selectedCourseProgramFile.set(null);
    this.coursForm.patchValue({
      code: '',
      nom: '',
      ects: null,
      note: null,
      lienProgramme: '',
      ueCodeAssocie: ''
    });
  }

  // Methode commune : on lance la requete, puis on recharge la page pour garder une vue coherente du dossier.
  private runRequestAndReload<T>(request$: Observable<T>, successMessage: string, onSuccess?: () => void) {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    request$.subscribe({
      next: () => {
        onSuccess?.();
        this.loadWorkspaceData(successMessage);
      },
      error: (error: HttpErrorResponse) => {
        console.error(error);
        this.saving.set(false);
        this.errorMessage.set(this.readApiErrorMessage(error, 'Operation impossible.'));
      }
    });
  }

  private readApiErrorMessage(error: unknown, fallback: string): string {
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

    return fallback;
  }
}
