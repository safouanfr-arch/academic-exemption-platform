package org.isfce.pid.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.isfce.pid.controller.error.DossierException;
import org.isfce.pid.dao.ICorrespondanceDao;
import org.isfce.pid.dao.ICoursExterneDao;
import org.isfce.pid.dao.ICoursReussiDao;
import org.isfce.pid.dao.IDemandeUEDao;
import org.isfce.pid.dao.IDocumentDao;
import org.isfce.pid.dao.IDossierDao;
import org.isfce.pid.dao.IEtablissementDao;
import org.isfce.pid.model.Correspondance;
import org.isfce.pid.model.CoursExterne;
import org.isfce.pid.model.CoursReussi;
import org.isfce.pid.model.DemandeUE;
import org.isfce.pid.model.Document;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.Etablissement;
import org.isfce.pid.model.Etablissement.StatutEtablissement;
import org.isfce.pid.model.EtatDossier;
import org.isfce.pid.model.NbDossiers;
import org.isfce.pid.model.Section;
import org.isfce.pid.model.TypeDocument;
import org.isfce.pid.model.UE;
import org.isfce.pid.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DossierService {

	private static final Path RACINE_UPLOAD = Paths.get(System.getProperty("user.home"), "PID_DB", "uploads")
			.toAbsolutePath()
			.normalize();

	private static final String ERR_COURS_REUTILISE =
			"Un meme cours reussi ne peut pas justifier plusieurs demandes UE";
	private static final String ERR_COURS_LIE_SUPPRESSION =
			"Le cours reussi est deja lie a une demande UE et ne peut pas etre supprime";
	private static final String ERR_LETTRE_MOTIVATION_MANQUANTE =
			"Lettre de motivation manquante";
	private static final String ERR_NOTE_INVALIDE =
			"Chaque cours justificatif doit avoir une note sur 20 comprise entre 10 et 20";

	private final IDossierDao dossierDao;
	private final IEtablissementDao etablissementDao;
	private final ICoursExterneDao coursExterneDao;
	private final ICoursReussiDao coursReussiDao;
	private final ICorrespondanceDao correspondanceDao;
	private final IDemandeUEDao demandeUeDao;
	private final IDocumentDao documentDao;

	// ==================== DOSSIERS ====================

	/**
	 * Cree un nouveau dossier.
	 * Regle metier :
	 * un etudiant peut avoir plusieurs dossiers au total,
	 * mais un seul dossier encore "en cours".
	 */
	public Dossier createDossier(User user, String objetDemande) throws DossierException {
		if (dossierDao.getNbDossierEnCours(user.getUsername()) > 0) {
			throw new DossierException("err.dossier.enCours");
		}

		Dossier newDossier = Dossier.builder()
				.date(LocalDate.now())
				.etat(EtatDossier.DEMANDE_EN_COURS)
				.user(user)
				.objetDemande(objetDemande)
				.build();

		return dossierDao.save(newDossier);
	}

	/**
	 * Variante pratique : on cree le dossier puis on lui attache directement une section.
	 */
	public Dossier createDossier(User user, String objetDemande, Section section) throws DossierException {
		Dossier newDossier = createDossier(user, objetDemande);
		newDossier.setSection(section);
		return dossierDao.save(newDossier);
	}

	public Optional<Dossier> getDossierEnCours(User user) {
		return dossierDao.findDossierEnCours(user);
	}

	public List<Dossier> getDossiersByUser(User user) {
		return dossierDao.findDossierByUserOrderByDateDesc(user);
	}

	public Optional<Dossier> getDossierById(Long id) {
		return dossierDao.findById(id);
	}

	public NbDossiers getNbDossiers(String userName) {
		return new NbDossiers(dossierDao.getNbDossierCloture(userName), dossierDao.getNbDossierEnCours(userName));
	}

	/**
	 * Un dossier n'est modifiable que dans les etats ou l'etudiant peut encore le construire.
	 */
	public void checkDossierEditable(Dossier dossier) throws DossierException {
		if (dossier.getEtat() != EtatDossier.DEMANDE_EN_COURS
				&& dossier.getEtat() != EtatDossier.ATTENTE_COMPLEMENT) {
			throw new DossierException(
					"Le dossier n est modifiable que dans les etats DEMANDE_EN_COURS ou ATTENTE_COMPLEMENT");
		}
	}

	public Dossier updateSection(Dossier dossier, Section section) {
		dossier.setSection(section);
		return dossierDao.save(dossier);
	}

	// ==================== COURS REUSSIS ====================

	public List<CoursReussi> getCoursReussis(Long dossierId) {
		return coursReussiDao.findByDossierId(dossierId);
	}

	/**
	 * CorrespondanceConnue = ce que l'on montre au front.
	 * On dit : "ce cours du dossier correspond a telle correspondance connue".
	 */
	public record CorrespondanceConnue(CoursReussi coursReussi, Correspondance correspondance) {}

	/**
	 * CorrespondanceTrouvee = structure interne temporaire.
	 * On garde la correspondance trouvee + les vrais cours du dossier qui la rendent possible.
	 */
	private record CorrespondanceTrouvee(Correspondance correspondance, List<CoursReussi> coursReussis) {}

	/**
	 * Detecte les correspondances automatiques applicables au dossier.
	 * Etapes de detection :
	 * 1. on regarde la section du dossier
	 * 2. on groupe les cours du dossier par ecole
	 * 3. on charge les correspondances connues de cette ecole
	 * 4. si tous les cours requis sont presents, la correspondance est applicable
	 */
	private List<CorrespondanceTrouvee> detecterCorrespondancesApplicables(Dossier dossier) {
		if (dossier.getSection() == null) {
			return List.of();
		}

		Set<String> codesUeAutorises = new HashSet<>();
		for (UE ue : dossier.getSection().getListeUE()) {    //Ici, on construit une liste des codes UE de la section du dossier.
			codesUeAutorises.add(ue.getCode());
		}

		Map<Long, List<CoursReussi>> coursParEtablissement = new LinkedHashMap<>();
		for (CoursReussi coursReussi : getCoursReussis(dossier.getId())) {
			CoursExterne coursExterneConnu = coursReussi.getCoursExterne();
			Etablissement etablissement = coursReussi.getEtablissement();    //Garder seulement les cours et etablissement connus dans notre table de cours externe connu pr correspondance

			// Une auto-correspondance n'est possible que pour un cours connu
			// rattache a une ecole de reference.
			if (coursExterneConnu == null || coursExterneConnu.getId() == null || etablissement == null
					|| etablissement.getId() == null) {
				continue;
			}

			coursParEtablissement.computeIfAbsent(etablissement.getId(), ignored -> new ArrayList<>())
					.add(coursReussi);
		}

		List<CorrespondanceTrouvee> correspondancesTrouvees = new ArrayList<>();

		for (List<CoursReussi> coursDeLEtablissement : coursParEtablissement.values()) {
			if (coursDeLEtablissement.isEmpty()) {
				continue;
			}

			Etablissement etablissement = coursDeLEtablissement.get(0).getEtablissement();
			List<Correspondance> correspondancesConnues = correspondanceDao.findByEtablissement(etablissement);

			for (Correspondance correspondanceConnue : correspondancesConnues) {   //Parcourir chaque correspondance connue par etablissement
				String codeUeVisee = correspondanceConnue.getUe().getCode();
				if (!codesUeAutorises.contains(codeUeVisee)) {
					continue;
				}

				Set<Long> idsCoursExternesRequis = new HashSet<>();
				for (CoursExterne coursExterneRequis : correspondanceConnue.getCoursExternes()) {
					if (coursExterneRequis.getId() != null) {
						idsCoursExternesRequis.add(coursExterneRequis.getId());
					}
				}

				if (idsCoursExternesRequis.isEmpty()) {
					continue;
				}

				List<CoursReussi> coursQuiCorrespondent = new ArrayList<>();     // les vrais CoursReussi du dossier qui matchent  ([IODA0101-2, IODA0103-1])
				Set<Long> idsCoursExternesPresents = new HashSet<>();

				for (CoursReussi coursDuDossier : coursDeLEtablissement) {
					Long idCoursExterne = coursDuDossier.getCoursExterne().getId();
					if (idsCoursExternesRequis.contains(idCoursExterne)) {
						coursQuiCorrespondent.add(coursDuDossier);
						idsCoursExternesPresents.add(idCoursExterne);
					}
				}

				if (idsCoursExternesPresents.containsAll(idsCoursExternesRequis)) {
					correspondancesTrouvees.add(new CorrespondanceTrouvee(correspondanceConnue, coursQuiCorrespondent));
				}
			}
		}

		return correspondancesTrouvees;
	}

	public List<CorrespondanceConnue> getCorrespondancesConnues(Dossier dossier) {
		List<CorrespondanceConnue> suggestions = new ArrayList<>();

		for (CorrespondanceTrouvee correspondanceTrouvee : detecterCorrespondancesApplicables(dossier)) {
			for (CoursReussi coursTrouve : correspondanceTrouvee.coursReussis()) {
				suggestions.add(new CorrespondanceConnue(coursTrouve, correspondanceTrouvee.correspondance()));
			}
		}

		return suggestions;
	}

	/**
	 * Applique vraiment les correspondances connues :
	 * - cree la demande UE si elle n'existe pas
	 * - lie les cours justificatifs trouves
	 */
	public List<DemandeUE> syncCorrespondancesConnues(Dossier dossier) throws DossierException {
		if (dossier.getSection() == null) {
			return List.of();
		}

		Map<String, DemandeUE> demandesParCodeUe = new LinkedHashMap<>();
		for (DemandeUE demande : demandeUeDao.findByDossier(dossier)) {
			demandesParCodeUe.put(demande.getUe().getCode(), demande);
		}

		Map<Long, DemandeUE> demandesSynchroniseesParId = new LinkedHashMap<>();

		for (CorrespondanceTrouvee correspondanceTrouvee : detecterCorrespondancesApplicables(dossier)) {
			UE ueVisee = correspondanceTrouvee.correspondance().getUe();
			DemandeUE demande = demandesParCodeUe.get(ueVisee.getCode());

			if (demande == null) {
				demande = addDemandeUE(dossier, ueVisee, null);
				demandesParCodeUe.put(ueVisee.getCode(), demande);
			}

			boolean demandeModifiee = false;
			for (CoursReussi coursTrouve : correspondanceTrouvee.coursReussis()) {
				verifierCoursPasDejaUtiliseAilleurs(demande, coursTrouve);
				if (demande.getCoursReussisJustificatifs().add(coursTrouve)) {  //on ajoute le cours dans la liste/set des cours justificatifs de la demande
					demandeModifiee = true;
				}
			}

			if (demandeModifiee) {
				demande = demandeUeDao.save(demande);
				demandesParCodeUe.put(ueVisee.getCode(), demande);
			}

			demandesSynchroniseesParId.put(demande.getId(), demande);
		}

		return new ArrayList<>(demandesSynchroniseesParId.values());
	}

	/**
	 * Regles minimales pour un cours inconnu.
	 * Si l'application ne connait pas deja le cours, l'etudiant doit fournir les informations utiles.
	 */
	private void verifierInfosCoursInconnu(String code, String nom, Integer ects, String prgm,
			String lienProgramme, boolean programmePdfFourni) throws DossierException {
		if (estVide(code) || estVide(nom) || ects == null || ects < 1) {
			throw new DossierException("Pour un cours inconnu, le code, le nom et les ECTS sont obligatoires");
		}

		if (estVide(prgm) && estVide(lienProgramme) && !programmePdfFourni) {
			throw new DossierException("Pour un cours inconnu, ajoute une URL de programme ou televerse un PDF");
		}
	}

	/**
	 * Point d'entree utilise par le portail etudiant.
	 * Cas 1 : cours connu dans la base -> on reutilise les donnees de reference.
	 * Cas 2 : cours inconnu -> on garde une version locale dans le dossier.
	 */
	public CoursReussi ajouterCoursReussiDepuisPortail(Dossier dossier, Long etablissementId, Long coursExterneId,
			String etablissementNom, String code, String nom, Integer ects, String note, String prgm,
			String lienProgramme, boolean programmePdfFourni) throws DossierException {
		String noteNormalisee = normaliserNoteSurVingt(note);

		if (coursExterneId != null) {
			CoursExterne coursExterneConnu = coursExterneDao.findById(coursExterneId)
					.orElseThrow(() -> new DossierException("Cours de reference introuvable"));

			CoursReussi coursConnu = CoursReussi.builder()
					.dossier(dossier)
					.etablissement(coursExterneConnu.getEtablissement())
					.coursExterne(coursExterneConnu)
					.code(coursExterneConnu.getCode())
					.nom(coursExterneConnu.getNom())
					.ects(coursExterneConnu.getEcts())
					.note(noteNormalisee)
					.build();

			return coursReussiDao.save(coursConnu);
		}

		Etablissement etablissementChoisi = null;
		String nomEtablissementLibre = null;

		if (etablissementId != null) {
			etablissementChoisi = etablissementDao.findById(etablissementId)
					.orElseThrow(() -> new DossierException("Etablissement de reference introuvable"));
		} else {
			if (estVide(etablissementNom)) {
				throw new DossierException("Le nom de l etablissement est obligatoire");
			}
			nomEtablissementLibre = etablissementNom.trim();
		}

		verifierInfosCoursInconnu(code, nom, ects, prgm, lienProgramme, programmePdfFourni);

		CoursReussi coursInconnu = CoursReussi.builder()
				.dossier(dossier)
				.etablissement(etablissementChoisi)
				.etablissementNomLibre(nomEtablissementLibre)
				.coursExterne(null)
				.code(code.trim())
				.nom(nom.trim())
				.ects(ects)
				.note(noteNormalisee)
				.prgm(estVide(prgm) ? null : prgm.trim())
				.lienProgramme(estVide(lienProgramme) ? null : lienProgramme.trim())
				.build();

		return coursReussiDao.save(coursInconnu);
	}

	/**
	 * Ancien point d'entree conserve pour les tests.
	 * Ici on garde le vieux comportement :
	 * un etablissement ou un cours inconnu peuvent etre crees dans la base de reference.
	 */
	public CoursReussi addCoursReussi(Dossier dossier, String etablissementNom, String code, String nom,
			int ects, String note, String prgm, String lienProgramme) throws DossierException {

		Etablissement etablissement = etablissementDao.findByNomIgnoreCase(etablissementNom)
				.orElseGet(() -> etablissementDao.save(Etablissement.builder()
						.nom(etablissementNom)
						.statut(StatutEtablissement.A_VERIFIER)
						.build()));

		CoursExterne coursExterne = coursExterneDao.findByEtablissementAndCodeIgnoreCase(etablissement, code)
				.orElse(null);

		if (coursExterne == null) {
			if (estVide(prgm) && estVide(lienProgramme)) {
				throw new DossierException("Programme ou lien vers le programme obligatoire pour un cours inconnu");
			}

			coursExterne = coursExterneDao.save(CoursExterne.builder()
					.etablissement(etablissement)
					.code(code)
					.nom(nom)
					.ects(ects)
					.prgm(prgm)
					.lienProgramme(lienProgramme)
					.build());
		}

		CoursReussi nouveauCours = CoursReussi.builder()
				.dossier(dossier)
				.etablissement(etablissement)
				.coursExterne(coursExterne)
				.code(code)
				.nom(nom)
				.ects(ects)
				.note(note)
				.build();

		return coursReussiDao.save(nouveauCours);
	}

	public void deleteCoursReussi(Long coursReussiId) throws DossierException {
		CoursReussi cours = coursReussiDao.findById(coursReussiId)
				.orElseThrow(() -> new java.util.NoSuchElementException("Cours non trouve: " + coursReussiId));

		if (trouverAutreDemandeQuiUtiliseLeCours(cours.getDossier(), cours, null) != null) {
			throw new DossierException(ERR_COURS_LIE_SUPPRESSION);
		}

		coursReussiDao.delete(cours);
	}

	public Optional<CoursReussi> getCoursReussiById(Long id) {
		return coursReussiDao.findById(id);
	}

	// ==================== DEMANDES UE ====================

	public List<DemandeUE> getDemandesUE(Long dossierId) {
		return demandeUeDao.findByDossierId(dossierId);
	}

	/**
	 * Une UE ne peut etre demandee qu'une seule fois dans un dossier.
	 * Et si le dossier a deja une section, l'UE doit appartenir a cette section.
	 */
	public DemandeUE addDemandeUE(Dossier dossier, UE ue, String commentaire) throws DossierException {
		if (demandeUeDao.existsByDossierAndUe(dossier, ue)) {
			throw new DossierException("Cette UE est deja demandee en dispense");
		}

		if (dossier.getSection() != null) {
			boolean ueDansLaSection = dossier.getSection().getListeUE().stream()
					.anyMatch(ueDeLaSection -> ueDeLaSection.getCode().equals(ue.getCode()));

			if (!ueDansLaSection) {
				throw new DossierException("L'UE ne fait pas partie de la section du dossier");
			}
		}

		DemandeUE newDemande = DemandeUE.builder()
				.dossier(dossier)
				.ue(ue)
				.commentaire(commentaire)
				.build();

		return demandeUeDao.save(newDemande);
	}

	public void deleteDemandeUE(Long demandeId) {
		demandeUeDao.deleteById(demandeId);
	}

	public Optional<DemandeUE> getDemandeUEById(Long id) {
		return demandeUeDao.findById(id);
	}

	public DemandeUE linkCoursToDemandeUE(DemandeUE demande, CoursReussi cours) throws DossierException {
		if (!cours.getDossier().getId().equals(demande.getDossier().getId())) {
			throw new DossierException("Le cours reussi n'appartient pas au meme dossier");
		}

		verifierCoursPasDejaUtiliseAilleurs(demande, cours);
		demande.getCoursReussisJustificatifs().add(cours);
		return demandeUeDao.save(demande);
	}

	/**
	 * Regle metier importante :
	 * un meme cours reussi ne peut pas servir de preuve pour deux demandes UE differentes.
	 */
	private void verifierCoursPasDejaUtiliseAilleurs(DemandeUE demande, CoursReussi cours)
			throws DossierException {
		DemandeUE autreDemande = trouverAutreDemandeQuiUtiliseLeCours(demande.getDossier(), cours, demande.getId());
		if (autreDemande != null) {
			throw new DossierException(ERR_COURS_REUTILISE);
		}
	}

	private DemandeUE trouverAutreDemandeQuiUtiliseLeCours(Dossier dossier, CoursReussi cours, Long demandeAIgnorerId) {
		for (DemandeUE demandeExistante : demandeUeDao.findByDossier(dossier)) {
			if (demandeAIgnorerId != null && demandeAIgnorerId.equals(demandeExistante.getId())) {
				continue;
			}

			for (CoursReussi coursLie : demandeExistante.getCoursReussisJustificatifs()) {
				if (coursLie.getId().equals(cours.getId())) {
					return demandeExistante;
				}
			}
		}

		return null;
	}

	public DemandeUE unlinkCoursFromDemandeUE(DemandeUE demande, CoursReussi cours) {
		demande.getCoursReussisJustificatifs().remove(cours);
		return demandeUeDao.save(demande);
	}

	// ==================== DOCUMENTS ====================

	public List<Document> getDocuments(Long dossierId) {
		return documentDao.findByDossierId(dossierId);
	}

	public Document addDocument(Dossier dossier, String nomFichier, TypeDocument typeDocument,
			String cheminFichier, String urlDocument, CoursReussi coursReussi) {
		Document documentStocke = Document.builder()
				.dossier(dossier)
				.nomFichier(nomFichier)
				.typeDocument(typeDocument)
				.cheminFichier(cheminFichier)
				.urlDocument(urlDocument)
				.coursReussi(coursReussi)
				.build();

		return documentDao.save(documentStocke);
	}

	/**
	 * On stocke les vrais fichiers sur disque, pas en BLOB en base.
	 * Chaque dossier a son propre sous-repertoire.
	 */
	public Document stockerDocumentTeleverse(Dossier dossier, MultipartFile fichier, TypeDocument typeDocument,
			CoursReussi coursReussi) throws IOException, DossierException {
		if (fichier == null || fichier.isEmpty()) {
			throw new DossierException("Aucun fichier transmis");
		}

		Path dossierDeDepot = RACINE_UPLOAD.resolve("dossier-" + dossier.getId()).normalize();
		Files.createDirectories(dossierDeDepot);

		String nomFichierOriginal = fichier.getOriginalFilename();
		String nomFichierNettoye = nettoyerNomFichierTeleverse(
				nomFichierOriginal == null || nomFichierOriginal.isBlank() ? "document" : nomFichierOriginal);
		String nomFichierStocke = dossier.getId() + "_" + UUID.randomUUID() + "_" + nomFichierNettoye;
		Path fichierCible = dossierDeDepot.resolve(nomFichierStocke).normalize();

		if (!fichierCible.startsWith(RACINE_UPLOAD)) {
			throw new IOException("Chemin de stockage invalide");
		}

		try (InputStream fluxFichier = fichier.getInputStream()) {
			Files.copy(fluxFichier, fichierCible, StandardCopyOption.REPLACE_EXISTING);
		}

		return addDocument(dossier, nomFichierNettoye, typeDocument, fichierCible.toString(), null, coursReussi);
	}

	public void supprimerDocument(Long documentId) {
		Document document = documentDao.findById(documentId).orElse(null);

		if (document != null && document.getCheminFichier() != null && !document.getCheminFichier().isBlank()) {
			Path cheminStocke = Paths.get(document.getCheminFichier()).toAbsolutePath().normalize();
			if (cheminStocke.startsWith(RACINE_UPLOAD)) {
				try {
					Files.deleteIfExists(cheminStocke);
				} catch (IOException exceptionIgnoree) {
					// On prefere garder la suppression en base meme si le disque ne se nettoie pas.
				}
			}
		}

		documentDao.deleteById(documentId);
	}

	public Optional<Document> getDocumentById(Long id) {
		return documentDao.findById(id);
	}

	// ==================== VALIDATION ET SOUMISSION ====================

	/**
	 * Resultat de la validation du dossier.
	 * complet = vrai seulement s'il ne reste aucune erreur bloquante.
	 */
	public record ValidationResult(boolean complet, List<String> erreurs, List<String> avertissements) {}

	/**
	 * Validation complete avant envoi.
	 * Regles verifiees :
	 * - une section
	 * - au moins une demande UE
	 * - chaque demande justifiee par des cours
	 * - lettre de motivation globale
	 * - bulletin global ou bulletin attache pour chaque cours justificatif
	 * - support pedagogique pour chaque cours justificatif
	 * - note valide sur 20
	 */
	public ValidationResult validateDossier(Long dossierId) {
		List<String> erreurs = new ArrayList<>();
		List<String> avertissements = new ArrayList<>();

		Dossier dossier = dossierDao.findById(dossierId).orElse(null);
		if (dossier == null) {
			erreurs.add("Dossier non trouve");
			return new ValidationResult(false, erreurs, avertissements);
		}

		if (dossier.getSection() == null) {
			erreurs.add("Section non definie");
		}

		List<DemandeUE> demandes = demandeUeDao.findByDossierId(dossierId);
		if (demandes.isEmpty()) {
			erreurs.add("Aucune UE demandee en dispense");
		}

		Map<Long, String> codeUeParCoursId = new LinkedHashMap<>();
		for (DemandeUE demande : demandes) {
			String codeUeActuel = demande.getUe().getCode();

			if (demande.getCoursReussisJustificatifs().isEmpty()) {
				erreurs.add("L'UE " + codeUeActuel + " n'est justifiee par aucun cours reussi");
			}

			for (CoursReussi cours : demande.getCoursReussisJustificatifs()) {
				String codeUePrecedent = codeUeParCoursId.putIfAbsent(cours.getId(), codeUeActuel);

				// Si un cours a deja servi pour une autre UE, on bloque.
				if (codeUePrecedent != null
						&& !codeUePrecedent.equals(codeUeActuel)
						&& !erreurs.contains(ERR_COURS_REUTILISE)) {
					erreurs.add(ERR_COURS_REUTILISE);
				}
			}
		}

		List<Document> documents = documentDao.findByDossierId(dossierId);
		if (!contientTypeDocument(documents, TypeDocument.LETTRE_MOTIVATION)) {
			erreurs.add(ERR_LETTRE_MOTIVATION_MANQUANTE);
		}

		for (DemandeUE demande : demandes) {
			for (CoursReussi cours : demande.getCoursReussisJustificatifs()) {
				if (!noteStockeeValide(cours.getNote()) && !erreurs.contains(ERR_NOTE_INVALIDE)) {
					erreurs.add(ERR_NOTE_INVALIDE);                     // note sur 20 comprise entre : 10 et 20
				}

				if (!aBulletinPourCours(documents, cours)) {
					erreurs.add("Le cours " + cours.getCode()
							+ " doit avoir un bulletin global ou un bulletin attache au cours");
				}

				if (!aSupportPedagogique(documents, cours)) {
					erreurs.add("Le cours " + cours.getCode()
							+ " doit avoir un dossier pedagogique ou une URL programme");
				}
			}
		}

		for (DemandeUE demande : demandes) {
			int ectsUe = demande.getUe().getEcts();
			int ectsCoursJustificatifs = demande.getCoursReussisJustificatifs().stream()
					.mapToInt(CoursReussi::getEcts)
					.sum();

			if (ectsCoursJustificatifs < ectsUe) {
				avertissements.add("UE " + demande.getUe().getCode() + " : " + ectsCoursJustificatifs
						+ " ECTS de cours vs " + ectsUe + " ECTS requis");
			}
		}

		return new ValidationResult(erreurs.isEmpty(), erreurs, avertissements);
	}

	/**
	 * Envoi definitif du dossier.
	 * On ne passe en TRAITEMENT_DIRECTION que si le dossier est complet.
	 */
	public Dossier soumettreDossier(Long dossierId) throws DossierException {
		Dossier dossier = dossierDao.findById(dossierId)
				.orElseThrow(() -> new DossierException("Dossier non trouve"));

		if (dossier.getEtat() != EtatDossier.DEMANDE_EN_COURS
				&& dossier.getEtat() != EtatDossier.ATTENTE_COMPLEMENT) {
			throw new DossierException("Le dossier n'est pas en etat DEMANDE_EN_COURS ou ATTENTE_COMPLEMENT");
		}

		ValidationResult validation = validateDossier(dossierId);
		if (!validation.complet()) {
			throw new DossierException("Dossier incomplet: " + String.join(", ", validation.erreurs()));
		}

		dossier.setEtat(EtatDossier.TRAITEMENT_DIRECTION);
		return dossierDao.save(dossier);
	}

	// ==================== PETITES METHODES LISIBLES ====================

	private boolean estVide(String valeur) {
		return valeur == null || valeur.isBlank();
	}

	private String normaliserNoteSurVingt(String note) throws DossierException {
		if (estVide(note)) {
			throw new DossierException("La note sur 20 est obligatoire");
		}

		String valeurBrute = note.trim().replace(',', '.');
		if (valeurBrute.endsWith("/20")) {
			valeurBrute = valeurBrute.substring(0, valeurBrute.length() - 3).trim();
		}

		BigDecimal valeurNumerique;
		try {
			valeurNumerique = new BigDecimal(valeurBrute);
		} catch (NumberFormatException ex) {
			throw new DossierException("La note doit etre un nombre sur 20");
		}

		if (valeurNumerique.compareTo(BigDecimal.TEN) < 0 || valeurNumerique.compareTo(new BigDecimal("20")) > 0) {
			throw new DossierException("La note doit etre comprise entre 10 et 20");
		}

		return valeurNumerique.stripTrailingZeros().toPlainString() + "/20";
	}

	private boolean noteStockeeValide(String note) {
		try {
			normaliserNoteSurVingt(note);
			return true;
		} catch (DossierException ex) {
			return false;
		}
	}

	private boolean contientTypeDocument(List<Document> documents, TypeDocument typeAttendu) {
		for (Document document : documents) {
			if (document.getTypeDocument() == typeAttendu) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Le bulletin peut etre global au dossier ou attache au cours concerne.
	 */
	private boolean aBulletinPourCours(List<Document> documents, CoursReussi cours) {
		for (Document document : documents) {
			if (document.getTypeDocument() != TypeDocument.BULLETIN) {
				continue;
			}
			if (document.getCoursReussi() == null) {
				return true;
			}
			if (document.getCoursReussi().getId().equals(cours.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Un cours est pedagogiquement justifie si on trouve :
	 * - soit un programme connu dans la base de reference
	 * - soit une info locale sur le cours du dossier
	 * - soit un document pedagogique attache a ce cours
	 */
	private boolean aSupportPedagogique(List<Document> documents, CoursReussi cours) {
		if (cours.getCoursExterne() != null) {
			String programmeConnu = cours.getCoursExterne().getPrgm();
			String urlProgrammeConnue = cours.getCoursExterne().getLienProgramme();
			if (!estVide(programmeConnu) || !estVide(urlProgrammeConnue)) {
				return true;
			}
		}

		if (!estVide(cours.getPrgm()) || !estVide(cours.getLienProgramme())) {
			return true;
		}

		for (Document document : documents) {
			if (document.getTypeDocument() != TypeDocument.DOSSIER_PEDAGOGIQUE) {
				continue;
			}
			if (document.getCoursReussi() == null || !document.getCoursReussi().getId().equals(cours.getId())) {
				continue;
			}
			if (!estVide(document.getCheminFichier()) || !estVide(document.getUrlDocument())) {
				return true;
			}
		}

		return false;
	}

	private String nettoyerNomFichierTeleverse(String nomFichier) {
		return nomFichier.replace("\\", "_")
				.replace("/", "_")
				.replace("..", "_")
				.trim();
	}
}
