package org.isfce.pid.controller;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.isfce.pid.controller.error.DossierException;
import org.isfce.pid.dao.SectionDao;
import org.isfce.pid.dao.UeDao;
import org.isfce.pid.dto.DossierDto;
import org.isfce.pid.mapper.UEMapper;
import org.isfce.pid.model.CoursReussi;
import org.isfce.pid.model.DemandeUE;
import org.isfce.pid.model.Document;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.NbDossiers;
import org.isfce.pid.model.Section;
import org.isfce.pid.model.TypeCorrespondance;
import org.isfce.pid.model.TypeDocument;
import org.isfce.pid.model.UE;
import org.isfce.pid.model.User;
import org.isfce.pid.service.DossierService;
import org.isfce.pid.service.UserService;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/dossier/", produces = "application/json")
@Slf4j
@CrossOrigin("*")
public class DossierController {

	private final DossierService dossierService;
	private final UserService userService;
	private final SectionDao sectionDao;
	private final UeDao ueDao;
	private final MessageSource messageSource;
	private final UEMapper ueMapper;

	public DossierController(DossierService dossierService, UserService userService, SectionDao sectionDao,
			UeDao ueDao, MessageSource messageSource, UEMapper ueMapper) {
		this.dossierService = dossierService;
		this.userService = userService;
		this.sectionDao = sectionDao;
		this.ueDao = ueDao;
		this.messageSource = messageSource;
		this.ueMapper = ueMapper;
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping(path = "add", consumes = "application/json")
	public ResponseEntity<DossierDto> creerDossier(@Valid @RequestBody DossierCreateRequest requete,
			Locale locale, Authentication authentication) throws NoSuchMessageException, DossierException {
		User utilisateurConnecte = chargerUtilisateurConnecte(authentication, locale);
		Section sectionChoisie = chargerSectionOptionnelle(requete.sectionCode);

		try {
			Dossier dossierCree = dossierService.createDossier(utilisateurConnecte, requete.objetDemande, sectionChoisie);
			return ResponseEntity.ok(ueMapper.toDossierDto(dossierCree));
		} catch (DossierException exception) {
			throw new DossierException(messageSource.getMessage(exception.getMessage(), new String[] {}, locale));
		}
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("actif")
	public ResponseEntity<DossierDto> consulterDossierActif(Authentication authentication) {
		User utilisateurConnecte = chargerUtilisateurConnecte(authentication);
		Dossier dossierActif = dossierService.getDossierEnCours(utilisateurConnecte)
				.orElseThrow(() -> new NoSuchElementException("Aucun dossier actif"));
		return ResponseEntity.ok(ueMapper.toDossierDto(dossierActif));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("liste")
	public ResponseEntity<List<DossierDto>> listerDossiersUtilisateur(Authentication authentication) {
		User utilisateurConnecte = chargerUtilisateurConnecte(authentication);
		List<DossierDto> dossiersDto = dossierService.getDossiersByUser(utilisateurConnecte).stream()
				.map(ueMapper::toDossierDto)
				.toList();
		return ResponseEntity.ok(dossiersDto);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PutMapping("{id}/section")
	public ResponseEntity<DossierDto> modifierSectionDossier(@PathVariable("id") Long dossierId,
			@Valid @RequestBody SectionUpdateRequest requete, Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);
		Section sectionChoisie = chargerSectionObligatoire(requete.sectionCode);
		Dossier dossierMisAJour = dossierService.updateSection(dossierEditable, sectionChoisie);
		return ResponseEntity.ok(ueMapper.toDossierDto(dossierMisAJour));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("nb")
	public ResponseEntity<NbDossiers> compterDossiers(Authentication authentication) {
		return ResponseEntity.ok(dossierService.getNbDossiers(authentication.getName()));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("{id}/validation")
	public ResponseEntity<ValidationResponse> consulterValidationDossier(@PathVariable("id") Long dossierId,
			Authentication authentication) {
		chargerDossierDuUserConnecte(dossierId, authentication);

		var resultatValidation = dossierService.validateDossier(dossierId);
		return ResponseEntity.ok(
				new ValidationResponse(resultatValidation.complet(), resultatValidation.erreurs(),
						resultatValidation.avertissements()));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("{id}/correspondances")
	public ResponseEntity<List<CorrespondanceConnueResponse>> consulterCorrespondancesConnues(
			@PathVariable("id") Long dossierId, Authentication authentication) {
		Dossier dossierUtilisateur = chargerDossierDuUserConnecte(dossierId, authentication);

		List<CorrespondanceConnueResponse> reponses = dossierService.getCorrespondancesConnues(dossierUtilisateur).stream()
				.map(this::versReponseCorrespondanceConnue)
				.toList();
		return ResponseEntity.ok(reponses);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping("{id}/correspondances/sync")
	public ResponseEntity<List<DemandeUEResponse>> synchroniserCorrespondancesConnues(@PathVariable("id") Long dossierId,
			Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);

		List<DemandeUEResponse> reponses = dossierService.syncCorrespondancesConnues(dossierEditable).stream()
				.map(this::versReponseDemande)
				.toList();
		return ResponseEntity.ok(reponses);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping("{id}/soumettre")
	public ResponseEntity<DossierDto> soumettreDossier(@PathVariable("id") Long dossierId, Authentication authentication)
			throws DossierException {
		chargerDossierDuUserConnecte(dossierId, authentication);
		Dossier dossierSoumis = dossierService.soumettreDossier(dossierId);
		return ResponseEntity.ok(ueMapper.toDossierDto(dossierSoumis));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("{id}/cours-reussis")
	public ResponseEntity<List<CoursReussiResponse>> listerCoursDossier(@PathVariable("id") Long dossierId,
			Authentication authentication) {
		chargerDossierDuUserConnecte(dossierId, authentication);

		List<CoursReussiResponse> reponses = dossierService.getCoursReussis(dossierId).stream()
				.map(this::versReponseCours)
				.toList();
		return ResponseEntity.ok(reponses);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping(path = "{id}/cours-reussis", consumes = "application/json")
	public ResponseEntity<CoursReussiResponse> ajouterCoursReussi(@PathVariable("id") Long dossierId,
			@Valid @RequestBody CoursReussiRequest requete, Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);

		log.info("Ajout cours reussi portal: etablissementId={}, coursExterneId={}, etablissementNom={}, code={}",
				requete.etablissementId, requete.coursExterneId, requete.etablissementNom, requete.code);

		CoursReussi coursCree = dossierService.ajouterCoursReussiDepuisPortail(dossierEditable, requete.etablissementId,
				requete.coursExterneId, requete.etablissementNom, requete.code, requete.nom, requete.ects,
				requete.note, requete.prgm, requete.lienProgramme,
				Boolean.TRUE.equals(requete.hasPedagogicalUpload));

		return new ResponseEntity<>(versReponseCours(coursCree), HttpStatus.CREATED);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@DeleteMapping("{id}/cours-reussis/{coursId}")
	public ResponseEntity<Void> supprimerCoursReussi(@PathVariable("id") Long dossierId, @PathVariable("coursId") Long coursId,
			Authentication authentication) throws DossierException {
		chargerDossierEditableDuUserConnecte(dossierId, authentication);
		chargerCoursDuDossier(dossierId, coursId);
		dossierService.deleteCoursReussi(coursId);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("{id}/demandes-ue")
	public ResponseEntity<List<DemandeUEResponse>> listerDemandesDossier(@PathVariable("id") Long dossierId,
			Authentication authentication) {
		chargerDossierDuUserConnecte(dossierId, authentication);

		List<DemandeUEResponse> reponses = dossierService.getDemandesUE(dossierId).stream()
				.map(this::versReponseDemande)
				.toList();
		return ResponseEntity.ok(reponses);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping(path = "{id}/demandes-ue", consumes = "application/json")
	public ResponseEntity<DemandeUEResponse> ajouterDemandeUe(@PathVariable("id") Long dossierId,
			@Valid @RequestBody DemandeUERequest requete, Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);
		UE ueDemandee = chargerUeObligatoire(requete.ueCode);

		log.info("Ajout demande UE: {} pour dossier {}", requete.ueCode, dossierId);
		DemandeUE demandeCree = dossierService.addDemandeUE(dossierEditable, ueDemandee, requete.commentaire);
		return new ResponseEntity<>(versReponseDemande(demandeCree), HttpStatus.CREATED);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@DeleteMapping("{id}/demandes-ue/{demandeId}")
	public ResponseEntity<Void> supprimerDemandeUe(@PathVariable("id") Long dossierId,
			@PathVariable("demandeId") Long demandeId, Authentication authentication) throws DossierException {
		chargerDossierEditableDuUserConnecte(dossierId, authentication);
		chargerDemandeDuDossier(dossierId, demandeId);
		dossierService.deleteDemandeUE(demandeId);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping("{id}/demandes-ue/{demandeId}/cours/{coursId}")
	public ResponseEntity<DemandeUEResponse> lierCoursADemande(@PathVariable("id") Long dossierId,
			@PathVariable("demandeId") Long demandeId, @PathVariable("coursId") Long coursId,
			Authentication authentication) throws DossierException {
		chargerDossierEditableDuUserConnecte(dossierId, authentication);
		DemandeUE demandeDuDossier = chargerDemandeDuDossier(dossierId, demandeId);
		CoursReussi coursDuDossier = chargerCoursDuDossier(dossierId, coursId);

		DemandeUE demandeMiseAJour = dossierService.linkCoursToDemandeUE(demandeDuDossier, coursDuDossier);
		return ResponseEntity.ok(versReponseDemande(demandeMiseAJour));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@DeleteMapping("{id}/demandes-ue/{demandeId}/cours/{coursId}")
	public ResponseEntity<DemandeUEResponse> retirerCoursDeDemande(@PathVariable("id") Long dossierId,
			@PathVariable("demandeId") Long demandeId, @PathVariable("coursId") Long coursId,
			Authentication authentication) throws DossierException {
		chargerDossierEditableDuUserConnecte(dossierId, authentication);
		DemandeUE demandeDuDossier = chargerDemandeDuDossier(dossierId, demandeId);
		CoursReussi coursDuDossier = chargerCoursDuDossier(dossierId, coursId);

		DemandeUE demandeMiseAJour = dossierService.unlinkCoursFromDemandeUE(demandeDuDossier, coursDuDossier);
		return ResponseEntity.ok(versReponseDemande(demandeMiseAJour));
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("{id}/documents")
	public ResponseEntity<List<DocumentResponse>> listerDocumentsDossier(@PathVariable("id") Long dossierId,
			Authentication authentication) {
		chargerDossierDuUserConnecte(dossierId, authentication);

		List<DocumentResponse> reponses = dossierService.getDocuments(dossierId).stream()
				.map(this::versReponseDocument)
				.toList();
		return ResponseEntity.ok(reponses);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping(path = "{id}/documents", consumes = "application/json")
	public ResponseEntity<DocumentResponse> ajouterReferenceDocument(@PathVariable("id") Long dossierId,
			@Valid @RequestBody DocumentRequest requete, Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);
		CoursReussi coursDuDossier = requete.coursReussiId != null
				? chargerCoursDuDossier(dossierId, requete.coursReussiId)
				: null;

		log.info("Ajout document: {} pour dossier {}", requete.nomFichier, dossierId);
		Document documentCree = dossierService.addDocument(dossierEditable, requete.nomFichier,
				requete.typeDocument, requete.cheminFichier, requete.urlDocument, coursDuDossier);
		return new ResponseEntity<>(versReponseDocument(documentCree), HttpStatus.CREATED);
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@PostMapping(path = "{id}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentResponse> televerserDocument(@PathVariable("id") Long dossierId,
			@RequestParam("file") MultipartFile fichierTeleverse,
			@RequestParam("typeDocument") TypeDocument typeDocument,
			@RequestParam(value = "coursReussiId", required = false) Long coursReussiId,
			Authentication authentication) throws DossierException {
		Dossier dossierEditable = chargerDossierEditableDuUserConnecte(dossierId, authentication);
		CoursReussi coursDuDossier = coursReussiId != null ? chargerCoursDuDossier(dossierId, coursReussiId) : null;

		try {
			Document documentStocke = dossierService.stockerDocumentTeleverse(dossierEditable, fichierTeleverse,
					typeDocument, coursDuDossier);
			return new ResponseEntity<>(versReponseDocument(documentStocke), HttpStatus.CREATED);
		} catch (DossierException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new DossierException("Upload du document impossible");
		}
	}

	@PreAuthorize("hasRole('ETUDIANT')")
	@DeleteMapping("{id}/documents/{docId}")
	public ResponseEntity<Void> supprimerDocument(@PathVariable("id") Long dossierId, @PathVariable("docId") Long docId,
			Authentication authentication) throws DossierException {
		chargerDossierEditableDuUserConnecte(dossierId, authentication);
		chargerDocumentDuDossier(dossierId, docId);
		dossierService.supprimerDocument(docId);
		return ResponseEntity.noContent().build();
	}

	private User chargerUtilisateurConnecte(Authentication authentication, Locale locale) {
		String username = authentication.getName();
		return userService.findUserByUsername(username)
				.orElseThrow(() -> new NoSuchElementException(
						messageSource.getMessage("err.notFound", new String[] {username}, locale)));
	}

	private User chargerUtilisateurConnecte(Authentication authentication) {
		return userService.findUserByUsername(authentication.getName())
				.orElseThrow(() -> new NoSuchElementException("Utilisateur non trouve"));
	}

	private Section chargerSectionOptionnelle(String sectionCode) {
		if (sectionCode == null || sectionCode.isBlank()) {
			return null;
		}
		return chargerSectionObligatoire(sectionCode);
	}

	private Section chargerSectionObligatoire(String sectionCode) {
		return sectionDao.findById(sectionCode)
				.orElseThrow(() -> new NoSuchElementException("Section non trouvee: " + sectionCode));
	}

	private UE chargerUeObligatoire(String ueCode) {
		return ueDao.findById(ueCode)
				.orElseThrow(() -> new NoSuchElementException("UE non trouvee: " + ueCode));
	}

	// Controle d'acces :
	// avant de lire un dossier, verifier qu'il appartient bien a l'utilisateur connecte.
	private Dossier chargerDossierDuUserConnecte(Long dossierId, Authentication authentication) {
		Dossier dossierTrouve = dossierService.getDossierById(dossierId)
				.orElseThrow(() -> new NoSuchElementException("Dossier non trouve: " + dossierId));

		if (!dossierTrouve.getUser().getUsername().equals(authentication.getName())) {
			throw new NoSuchElementException("Acces non autorise au dossier");
		}
		return dossierTrouve;
	}

	// Controle de modification :
	// pour modifier, il faut etre proprietaire du dossier et le dossier doit etre modifiable.
	private Dossier chargerDossierEditableDuUserConnecte(Long dossierId, Authentication authentication) throws DossierException {
		Dossier dossierUtilisateur = chargerDossierDuUserConnecte(dossierId, authentication);
		dossierService.checkDossierEditable(dossierUtilisateur);
		return dossierUtilisateur;
	}

	// Controle de coherence :
	// l'id du cours dans l'URL doit appartenir au dossier indique dans l'URL.
	private CoursReussi chargerCoursDuDossier(Long dossierId, Long coursId) {
		CoursReussi coursTrouve = dossierService.getCoursReussiById(coursId)
				.orElseThrow(() -> new NoSuchElementException("Cours non trouve: " + coursId));

		if (!coursTrouve.getDossier().getId().equals(dossierId)) {
			throw new NoSuchElementException("Cours n'appartient pas au dossier");
		}
		return coursTrouve;
	}

	private DemandeUE chargerDemandeDuDossier(Long dossierId, Long demandeId) {
		DemandeUE demandeTrouvee = dossierService.getDemandeUEById(demandeId)
				.orElseThrow(() -> new NoSuchElementException("Demande non trouvee: " + demandeId));

		if (!demandeTrouvee.getDossier().getId().equals(dossierId)) {
			throw new NoSuchElementException("Demande n'appartient pas au dossier");
		}
		return demandeTrouvee;
	}

	private Document chargerDocumentDuDossier(Long dossierId, Long documentId) {
		Document documentTrouve = dossierService.getDocumentById(documentId)
				.orElseThrow(() -> new NoSuchElementException("Document non trouve: " + documentId));

		if (!documentTrouve.getDossier().getId().equals(dossierId)) {
			throw new NoSuchElementException("Document n'appartient pas au dossier");
		}
		return documentTrouve;
	}

	// Methode d'affichage :
	// si l'ecole est connue, on affiche le nom de reference.
	// sinon, on affiche le nom libre encode par l'etudiant.
	private String nomEtablissementAffiche(CoursReussi cours) {
		if (cours.getEtablissement() != null) {
			return cours.getEtablissement().getNom();
		}
		return cours.getEtablissementNomLibre();
	}

	private CoursReussiResponse versReponseCours(CoursReussi cours) {
		String programmeStocke = cours.getCoursExterne() != null
				? cours.getCoursExterne().getPrgm()
				: cours.getPrgm();
		String urlProgrammeStockee = cours.getCoursExterne() != null
				? cours.getCoursExterne().getLienProgramme()
				: cours.getLienProgramme();

		return new CoursReussiResponse(cours.getId(), nomEtablissementAffiche(cours), cours.getCode(),
				cours.getNom(), cours.getEcts(), cours.getNote(), programmeStocke, urlProgrammeStockee);
	}

	private DemandeUEResponse versReponseDemande(DemandeUE demande) {
		List<Long> idsCoursLies = demande.getCoursReussisJustificatifs().stream()
				.map(CoursReussi::getId)
				.toList();

		return new DemandeUEResponse(demande.getId(), demande.getUe().getCode(), demande.getUe().getNom(),
				demande.getUe().getEcts(), demande.getCommentaire(), idsCoursLies);
	}

	private DocumentResponse versReponseDocument(Document document) {
		Long idCoursLie = document.getCoursReussi() != null ? document.getCoursReussi().getId() : null;

		return new DocumentResponse(document.getId(), document.getNomFichier(), document.getTypeDocument(),
				document.getCheminFichier(), document.getUrlDocument(), idCoursLie);
	}

	private CorrespondanceConnueResponse versReponseCorrespondanceConnue(
			DossierService.CorrespondanceConnue suggestion) {
		CoursReussi cours = suggestion.coursReussi();
		var correspondance = suggestion.correspondance();

		return new CorrespondanceConnueResponse(cours.getId(), cours.getCode(), cours.getNom(),
				nomEtablissementAffiche(cours), correspondance.getUe().getCode(), correspondance.getUe().getNom(),
				correspondance.getUe().getEcts(), correspondance.getTypeCorrespondance(),
				correspondance.getCommentaire());
	}

	// Records HTTP utilises uniquement pour les echanges avec l'interface.
	record DossierCreateRequest(@NotBlank String objetDemande, String sectionCode) {}

	record SectionUpdateRequest(@NotBlank String sectionCode) {}

	record CoursReussiRequest(Long etablissementId, Long coursExterneId, String etablissementNom, String code,
			String nom, Integer ects, String note, String prgm, String lienProgramme, Boolean hasPedagogicalUpload) {}

	record CoursReussiResponse(Long id, String etablissementNom, String code, String nom, int ects, String note,
			String prgm, String lienProgramme) {}

	record DemandeUERequest(@NotBlank String ueCode, String commentaire) {}

	record DemandeUEResponse(Long id, String ueCode, String ueNom, int ueEcts, String commentaire,
			List<Long> coursReussisIds) {}

	record DocumentRequest(@NotBlank String nomFichier, TypeDocument typeDocument, String cheminFichier,
			String urlDocument, Long coursReussiId) {}

	record DocumentResponse(Long id, String nomFichier, TypeDocument typeDocument, String cheminFichier,
			String urlDocument, Long coursReussiId) {}

	record CorrespondanceConnueResponse(Long coursReussiId, String coursCode, String coursNom,
			String etablissementNom, String ueCode, String ueNom, int ueEcts,
			TypeCorrespondance typeCorrespondance, String commentaire) {}

	record ValidationResponse(boolean complet, List<String> erreurs, List<String> avertissements) {}
}
