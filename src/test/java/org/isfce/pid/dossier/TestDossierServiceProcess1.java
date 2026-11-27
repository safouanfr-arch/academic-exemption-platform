package org.isfce.pid.dossier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.isfce.pid.controller.error.DossierException;
import org.isfce.pid.dao.ICoursExterneDao;
import org.isfce.pid.dao.ICorrespondanceDao;
import org.isfce.pid.dao.IDossierDao;
import org.isfce.pid.dao.IEtablissementDao;
import org.isfce.pid.dao.IUserDao;
import org.isfce.pid.dao.SectionDao;
import org.isfce.pid.dao.UeDao;
import org.isfce.pid.model.Correspondance;
import org.isfce.pid.model.CoursExterne;
import org.isfce.pid.model.CoursReussi;
import org.isfce.pid.model.DemandeUE;
import org.isfce.pid.model.Document;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.Etablissement;
import org.isfce.pid.model.Etablissement.StatutEtablissement;
import org.isfce.pid.model.EtatDossier;
import org.isfce.pid.model.Section;
import org.isfce.pid.model.TypeCorrespondance;
import org.isfce.pid.model.TypeDocument;
import org.isfce.pid.model.UE;
import org.isfce.pid.model.User;
import org.isfce.pid.service.DossierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testU")
@Sql(scripts = "/dataTestU.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class TestDossierServiceProcess1 {

	private static final String ERR_SECTION_MANQUANTE = "Section non definie";
	private static final String ERR_AUCUNE_UE = "Aucune UE demandee en dispense";
	private static final String ERR_AUCUN_COURS_LIE = "L'UE IPAP n'est justifiee par aucun cours reussi";
	private static final String ERR_LETTRE_MOTIVATION_MANQUANTE = "Lettre de motivation manquante";
	private static final String ERR_COURS_INCONNU_SANS_PROGRAMME =
			"Programme ou lien vers le programme obligatoire pour un cours inconnu";
	private static final String ERR_UE_HORS_SECTION =
			"L'UE ne fait pas partie de la section du dossier";
	private static final String ERR_UE_DOUBLON =
			"Cette UE est deja demandee en dispense";
	private static final String ERR_DOSSIER_NON_EDITABLE =
			"Le dossier n est modifiable que dans les etats DEMANDE_EN_COURS ou ATTENTE_COMPLEMENT";
	private static final String ERR_ETAT_SOUMISSION =
			"Le dossier n'est pas en etat DEMANDE_EN_COURS ou ATTENTE_COMPLEMENT";
	private static final String ERR_COURS_REUTILISE =
			"Un meme cours reussi ne peut pas justifier plusieurs demandes UE";
	private static final String ERR_COURS_LIE_SUPPRESSION =
			"Le cours reussi est deja lie a une demande UE et ne peut pas etre supprime";

	@Autowired
	private DossierService dossierService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IDossierDao dossierDao;

	@Autowired
	private IEtablissementDao etablissementDao;

	@Autowired
	private ICoursExterneDao coursExterneDao;

	@Autowired
	private ICorrespondanceDao correspondanceDao;

	@Autowired
	private IUserDao userDao;

	@Autowired
	private SectionDao sectionDao;

	@Autowired
	private UeDao ueDao;

	@Test
	void soumettreDossier_sansSection_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierSansSection("no_section");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addRequiredDocuments(dossier);

		assertValidationErrors(dossier, ERR_SECTION_MANQUANTE);
		assertSubmissionRefused(dossier, ERR_SECTION_MANQUANTE);
	}

	@Test
	void soumettreDossier_sansDemandeUE_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_demande", "INFO");
		addRequiredDocuments(dossier);

		assertValidationErrors(dossier, ERR_AUCUNE_UE);
		assertSubmissionRefused(dossier, ERR_AUCUNE_UE);
	}

	@Test
	void soumettreDossier_demandeSansCoursLie_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_linked_course", "INFO");
		addIpapDemande(dossier);
		addRequiredDocuments(dossier);

		assertValidationErrors(dossier, ERR_AUCUN_COURS_LIE);
		assertSubmissionRefused(dossier, ERR_AUCUN_COURS_LIE);
	}

	@Test
	void soumettreDossier_sansDocument_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_document", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);

		assertValidationErrors(dossier, ERR_LETTRE_MOTIVATION_MANQUANTE, bulletinError(cours));
		assertSubmissionRefused(dossier, ERR_LETTRE_MOTIVATION_MANQUANTE, bulletinError(cours));
	}

	@Test
	void soumettreDossier_sansLettreMotivation_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_lettre", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addBulletin(dossier);

		assertValidationErrors(dossier, ERR_LETTRE_MOTIVATION_MANQUANTE);
		assertSubmissionRefused(dossier, ERR_LETTRE_MOTIVATION_MANQUANTE);
	}

	@Test
	void soumettreDossier_sansBulletin_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_bulletin", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addLettreMotivation(dossier);

		assertValidationErrors(dossier, bulletinError(cours));
		assertSubmissionRefused(dossier, bulletinError(cours));
	}

	@Test
	void soumettreDossier_avecBulletinLieAuCours_passe() throws Exception {
		Dossier dossier = createDossierAvecSection("bulletin_lie_cours", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addLettreMotivation(dossier);
		addBulletin(dossier, cours);

		var validation = dossierService.validateDossier(dossier.getId());
		assertTrue(validation.complet());
		assertTrue(validation.erreurs().isEmpty());
	}

	@Test
	void soumettreDossier_sansSupportPedagogiqueRefuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("no_pedagogical_support", "INFO");
		CoursReussi cours = addKnownCourseWithoutProgramSupport(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addRequiredDocuments(dossier);

		String expectedError = pedagogicalSupportError(cours);
		assertValidationErrors(dossier, expectedError);
		assertSubmissionRefused(dossier, expectedError);
	}

	@Test
	void soumettreDossier_avecDossierPedagogiqueLieAuCours_passe() throws Exception {
		Dossier dossier = createDossierAvecSection("with_pedagogical_doc", "INFO");
		CoursReussi cours = addKnownCourseWithoutProgramSupport(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addRequiredDocuments(dossier);
		addDossierPedagogique(dossier, cours);

		var validation = dossierService.validateDossier(dossier.getId());
		assertTrue(validation.complet());
		assertTrue(validation.erreurs().isEmpty());
	}

	@Test
	void soumettreDossier_dossierComplet_passeEnTraitementDirection() throws Exception {
		Dossier dossier = createDossierAvecSection("complete", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		Document document = addRequiredDocuments(dossier);

		assertNotNull(document.getId());

		var validation = dossierService.validateDossier(dossier.getId());
		assertTrue(validation.complet());
		assertTrue(validation.erreurs().isEmpty());
		assertTrue(validation.avertissements().isEmpty());

		Dossier soumis = dossierService.soumettreDossier(dossier.getId());
		assertEquals(EtatDossier.TRAITEMENT_DIRECTION, soumis.getEtat());
		assertEquals(EtatDossier.TRAITEMENT_DIRECTION,
				dossierService.getDossierById(dossier.getId()).orElseThrow().getEtat());
	}

	@Test
	void soumettreDossier_attenteComplementComplet_passeEnTraitementDirection() throws Exception {
		Dossier dossier = createDossierAvecSection("complete_attente_complement", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addRequiredDocuments(dossier);
		dossier.setEtat(EtatDossier.ATTENTE_COMPLEMENT);
		dossierDao.saveAndFlush(dossier);

		var validation = dossierService.validateDossier(dossier.getId());
		assertTrue(validation.complet());

		Dossier soumis = dossierService.soumettreDossier(dossier.getId());
		assertEquals(EtatDossier.TRAITEMENT_DIRECTION, soumis.getEtat());
	}

	@Test
	void soumettreDossier_attenteComplementIncomplet_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("incomplete_attente_complement", "INFO");
		dossier.setEtat(EtatDossier.ATTENTE_COMPLEMENT);
		dossierDao.saveAndFlush(dossier);

		assertValidationErrors(dossier, ERR_AUCUNE_UE, ERR_LETTRE_MOTIVATION_MANQUANTE);
		assertSubmissionRefused(dossier, ERR_AUCUNE_UE, ERR_LETTRE_MOTIVATION_MANQUANTE);
	}

	@Test
	void soumettreDossier_autresEtats_refuseLaSoumission() throws Exception {
		Dossier dossier = createDossierAvecSection("wrong_submit_state", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);
		addRequiredDocuments(dossier);

		for (EtatDossier etat : List.of(EtatDossier.TRAITEMENT_DIRECTION, EtatDossier.TRAITEMENT_PROFESSEUR,
				EtatDossier.TRAITE, EtatDossier.CLOTURE)) {
			dossier.setEtat(etat);
			dossierDao.saveAndFlush(dossier);

			DossierException exc = assertThrows(DossierException.class,
					() -> dossierService.soumettreDossier(dossier.getId()));

			assertEquals(ERR_ETAT_SOUMISSION, exc.getMessage());
		}
	}

	@Test
	void addCoursReussi_nouveauCoursSansProgrammeNiLien_refuse() throws Exception {
		Dossier dossier = createDossierAvecSection("cours_sans_prgm", "INFO");
		String suffix = uniqueSuffix();

		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.addCoursReussi(dossier, "Etab " + suffix, "CR" + suffix,
						"Cours inconnu " + suffix, 5, "12/20", null, null));

		assertEquals(ERR_COURS_INCONNU_SANS_PROGRAMME, exc.getMessage());
	}

	@Test
	void addDemandeUE_ueHorsSection_refuse() throws Exception {
		Dossier dossier = createDossierAvecSection("ue_hors_section", "COMPTA");

		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.addDemandeUE(dossier, getUe("IPAP"), "UE hors section"));

		assertEquals(ERR_UE_HORS_SECTION, exc.getMessage());
	}

	@Test
	void addDemandeUE_doublon_refuse() throws Exception {
		Dossier dossier = createDossierAvecSection("ue_doublon", "INFO");
		UE ue = getUe("IPAP");

		dossierService.addDemandeUE(dossier, ue, "Premiere demande");

		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.addDemandeUE(dossier, ue, "Deuxieme demande"));

		assertEquals(ERR_UE_DOUBLON, exc.getMessage());
	}

	@Test
	void getDossiersByUser_retourneUniquementLesDossiersDuUserTriesParDateDesc() {
		User user = userDao.findById("et1")
				.orElseThrow(() -> new AssertionError("Utilisateur de test absent: et1"));

		List<Dossier> dossiers = dossierService.getDossiersByUser(user);

		assertEquals(3, dossiers.size());
		assertTrue(dossiers.stream().allMatch(d -> d.getUser().getUsername().equals("et1")));
		assertIterableEquals(
				List.of("2026-01-22", "2026-01-19", "2025-01-10"),
				dossiers.stream().map(d -> d.getDate().toString()).toList());
	}

	@Test
	void checkDossierEditable_demandeEnCoursEtAttenteComplement_autorise() throws Exception {
		Dossier dossier = createDossierAvecSection("editable_states", "INFO");

		dossierService.checkDossierEditable(dossier);

		dossier.setEtat(EtatDossier.ATTENTE_COMPLEMENT);
		dossierService.checkDossierEditable(dossier);
	}

	@Test
	void checkDossierEditable_etatsNonEditables_refuse() throws Exception {
		Dossier dossier = createDossierAvecSection("non_editable_states", "INFO");

		for (EtatDossier etat : List.of(EtatDossier.TRAITEMENT_DIRECTION, EtatDossier.TRAITEMENT_PROFESSEUR,
				EtatDossier.TRAITE, EtatDossier.CLOTURE)) {
			dossier.setEtat(etat);

			DossierException exc = assertThrows(DossierException.class,
					() -> dossierService.checkDossierEditable(dossier));

			assertEquals(ERR_DOSSIER_NON_EDITABLE, exc.getMessage());
		}
	}

	@Test
	void linkCoursToDemandeUE_refuseSousRessourcesDAutreDossier() throws Exception {
		User owner = createDedicatedUser("owner_link");
		Dossier dossierOwner = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));

		User other = createDedicatedUser("other_link");
		Dossier otherDossier = dossierService.createDossier(other, "Demande other", getSection("INFO"));
		CoursReussi otherCours = addValidCourse(otherDossier);
		DemandeUE otherDemande = addIpapDemande(otherDossier);

		dossierDao.flush();

		mockMvc.perform(post("/api/dossier/" + dossierOwner.getId() + "/demandes-ue/" + otherDemande.getId()
						+ "/cours/" + otherCours.getId())
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void linkCoursToDemandeUE_refuseReutilisationDuMemeCoursPourDeuxDemandes() throws Exception { // Teste la logique métier de refus de réutilisation d'un même cours réussi pour justifier plusieurs demandes UE différentes du même dossier
		Dossier dossier = createDossierAvecSection("link_reuse_guard", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demandeIpap = addIpapDemande(dossier);
		DemandeUE demandeIpid = addIpidDemande(dossier);

		dossierService.linkCoursToDemandeUE(demandeIpap, cours);

		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.linkCoursToDemandeUE(demandeIpid, cours));

		assertEquals(ERR_COURS_REUTILISE, exc.getMessage());
	}

	@Test
	void linkCoursToDemandeUE_endpointRefuseReutilisationDuMemeCoursPourDeuxDemandes() throws Exception { // Teste que l'endpoint de liaison d'un cours réussi à une demande UE refuse également la réutilisation d'un même cours pour justifier plusieurs demandes UE différentes du même dossier
		User owner = createDedicatedUser("owner_link_reuse");
		Dossier dossier = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demandeIpap = addIpapDemande(dossier);
		DemandeUE demandeIpid = addIpidDemande(dossier);
		dossierService.linkCoursToDemandeUE(demandeIpap, cours);

		mockMvc.perform(post("/api/dossier/" + dossier.getId() + "/demandes-ue/" + demandeIpid.getId()
						+ "/cours/" + cours.getId())
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.objetDemande").value(ERR_COURS_REUTILISE));
	}

	@Test
	void unlinkCoursFromDemandeUE_refuseSousRessourcesDAutreDossier() throws Exception {
		User owner = createDedicatedUser("owner_unlink");
		Dossier dossierOwner = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));

		User other = createDedicatedUser("other_unlink");
		Dossier otherDossier = dossierService.createDossier(other, "Demande other", getSection("INFO"));
		CoursReussi otherCours = addValidCourse(otherDossier);
		DemandeUE otherDemande = addIpapDemande(otherDossier);
		dossierService.linkCoursToDemandeUE(otherDemande, otherCours);

		dossierDao.flush();

		mockMvc.perform(delete("/api/dossier/" + dossierOwner.getId() + "/demandes-ue/" + otherDemande.getId()
						+ "/cours/" + otherCours.getId())
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteCoursReussi_refuseSuppressionSiCoursDejaLie() throws Exception {
		Dossier dossier = createDossierAvecSection("delete_linked_course", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);

		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.deleteCoursReussi(cours.getId()));

		assertEquals(ERR_COURS_LIE_SUPPRESSION, exc.getMessage());
	}

	@Test
	void deleteCoursReussi_endpointRefuseSuppressionSiCoursDejaLie() throws Exception {
		User owner = createDedicatedUser("owner_delete_linked");
		Dossier dossier = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		DemandeUE demande = addIpapDemande(dossier);
		dossierService.linkCoursToDemandeUE(demande, cours);

		mockMvc.perform(delete("/api/dossier/" + dossier.getId() + "/cours-reussis/" + cours.getId())
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.objetDemande").value(ERR_COURS_LIE_SUPPRESSION));
	}

	@Test
	void getCorrespondancesConnues_dossierSansSection_retourneVide() throws Exception {
		Dossier dossier = createDossierSansSection("correspondances_no_section");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance connue");

		List<DossierService.CorrespondanceConnue> correspondances = dossierService.getCorrespondancesConnues(dossier);

		assertTrue(correspondances.isEmpty());
	}

	@Test
	void getCorrespondancesConnues_filtreLesUeHorsSectionDuDossier() throws Exception {
		Dossier dossier = createDossierAvecSection("correspondances_wrong_section", "COMPTA");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance hors section");

		List<DossierService.CorrespondanceConnue> correspondances = dossierService.getCorrespondancesConnues(dossier);

		assertTrue(correspondances.isEmpty());
	}

	@Test
	void getCorrespondancesConnues_correspondanceMultiCoursIncomplete_retourneVide() throws Exception {
		Dossier dossier = createDossierAvecSection("correspondances_multi_incomplete", "INFO");
		String suffix = uniqueSuffix();
		String etablissementNom = "Etab multi " + suffix;
		CoursReussi cours1 = addCourse(dossier, etablissementNom, "CM1" + suffix, "Cours multi 1", 4);

		Dossier autreDossier = createDossierAvecSection("correspondances_multi_seed", "INFO");
		CoursReussi cours2 = addCourse(autreDossier, etablissementNom, "CM2" + suffix, "Cours multi 2", 4);
		createCorrespondance(List.of(cours1, cours2), getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE,
				"Correspondance multi");

		List<DossierService.CorrespondanceConnue> correspondances = dossierService.getCorrespondancesConnues(dossier);

		assertTrue(correspondances.isEmpty());
	}

	@Test
	void syncCorrespondancesConnues_dossierSansSection_neCreeRien() throws Exception {
		Dossier dossier = createDossierSansSection("sync_no_section");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance connue");

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);

		assertTrue(synced.isEmpty());
		assertTrue(dossierService.getDemandesUE(dossier.getId()).isEmpty());
	}

	@Test
	void syncCorrespondancesConnues_correspondanceSimple_creeDemandeEtLieCours() throws Exception {
		Dossier dossier = createDossierAvecSection("sync_simple", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance simple");

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);
		List<DemandeUE> demandes = dossierService.getDemandesUE(dossier.getId());

		assertEquals(1, synced.size());
		assertEquals(1, demandes.size());
		assertEquals("IPAP", demandes.get(0).getUe().getCode());
		assertEquals(1, demandes.get(0).getCoursReussisJustificatifs().size());
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours.getId())));
	}

	@Test
	void syncCorrespondancesConnues_relancee_resteIdempotente() throws Exception {
		Dossier dossier = createDossierAvecSection("sync_idempotent", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance simple");

		List<DemandeUE> firstSync = dossierService.syncCorrespondancesConnues(dossier);
		List<DemandeUE> secondSync = dossierService.syncCorrespondancesConnues(dossier);
		List<DemandeUE> demandes = dossierService.getDemandesUE(dossier.getId());

		assertEquals(1, firstSync.size());
		assertEquals(1, secondSync.size());
		assertEquals(1, demandes.size());
		assertEquals(1, demandes.get(0).getCoursReussisJustificatifs().size());
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours.getId())));
	}

	@Test
	void syncCorrespondancesConnues_demandeExistante_reutiliseLaDemande() throws Exception {
		Dossier dossier = createDossierAvecSection("sync_existing_demande", "INFO");
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Correspondance simple");
		DemandeUE demandeManuelle = addIpapDemande(dossier);

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);
		List<DemandeUE> demandes = dossierService.getDemandesUE(dossier.getId());

		assertEquals(1, synced.size());
		assertEquals(1, demandes.size());
		assertEquals(demandeManuelle.getId(), demandes.get(0).getId());
		assertEquals(1, demandes.get(0).getCoursReussisJustificatifs().size());
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours.getId())));
	}

	@Test
	void syncCorrespondancesConnues_correspondanceMultiCoursComplete_creeDemandeEtLieTousLesCours() throws Exception {
		Dossier dossier = createDossierAvecSection("sync_multi_complete", "INFO");
		String suffix = uniqueSuffix();
		String etablissementNom = "Etab sync multi " + suffix;
		CoursReussi cours1 = addCourse(dossier, etablissementNom, "SC1" + suffix, "Sync cours 1", 4);
		CoursReussi cours2 = addCourse(dossier, etablissementNom, "SC2" + suffix, "Sync cours 2", 4);
		createCorrespondance(List.of(cours1, cours2), getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE,
				"Correspondance multi");

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);
		List<DemandeUE> demandes = dossierService.getDemandesUE(dossier.getId());

		assertEquals(1, synced.size());
		assertEquals(1, demandes.size());
		assertEquals(2, demandes.get(0).getCoursReussisJustificatifs().size());
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours1.getId())));
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours2.getId())));
	}

	@Test
	void getCorrespondancesConnues_endpointRetourneSuggestionsFiltreesParSection() throws Exception {
		User owner = createDedicatedUser("correspondances_endpoint");
		Dossier dossier = dossierService.createDossier(owner, "Demande correspondances", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Dispense connue IPAP");

		mockMvc.perform(get("/api/dossier/" + dossier.getId() + "/correspondances")
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].coursReussiId").value(cours.getId()))
				.andExpect(jsonPath("$[0].coursCode").value(cours.getCode()))
				.andExpect(jsonPath("$[0].etablissementNom").value(cours.getEtablissement().getNom()))
				.andExpect(jsonPath("$[0].ueCode").value("IPAP"))
				.andExpect(jsonPath("$[0].typeCorrespondance").value("DISPENSE_TOTALE"))
				.andExpect(jsonPath("$[0].commentaire").value("Dispense connue IPAP"));
	}

	@Test
	void getCorrespondancesConnues_nonProprietaire_refuseAcces() throws Exception {
		User owner = createDedicatedUser("correspondances_owner");
		Dossier dossier = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Dispense connue IPAP");

		User other = createDedicatedUser("correspondances_other");

		mockMvc.perform(get("/api/dossier/" + dossier.getId() + "/correspondances")
				.with(authentication(tokenFor(other.getUsername()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void syncCorrespondancesConnues_endpointNonProprietaire_refuseAcces() throws Exception {
		User owner = createDedicatedUser("sync_owner");
		Dossier dossier = dossierService.createDossier(owner, "Demande owner", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Dispense connue IPAP");

		User other = createDedicatedUser("sync_other");

		mockMvc.perform(post("/api/dossier/" + dossier.getId() + "/correspondances/sync")
				.with(authentication(tokenFor(other.getUsername()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void syncCorrespondancesConnues_endpointDossierNonEditable_refuse() throws Exception {
		User owner = createDedicatedUser("sync_non_editable");
		Dossier dossier = dossierService.createDossier(owner, "Demande non editable", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Dispense connue IPAP");
		dossier.setEtat(EtatDossier.TRAITEMENT_DIRECTION);
		dossierDao.saveAndFlush(dossier);

		mockMvc.perform(post("/api/dossier/" + dossier.getId() + "/correspondances/sync")
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.objetDemande").value(ERR_DOSSIER_NON_EDITABLE));
	}

	@Test
	void syncCorrespondancesConnues_endpointCreeEtEnrichitLesDemandesAttendues() throws Exception {
		User owner = createDedicatedUser("sync_endpoint");
		Dossier dossier = dossierService.createDossier(owner, "Demande sync endpoint", getSection("INFO"));
		CoursReussi cours = addValidCourse(dossier);
		createCorrespondance(cours, getUe("IPAP"), TypeCorrespondance.DISPENSE_TOTALE, "Dispense connue IPAP");

		mockMvc.perform(post("/api/dossier/" + dossier.getId() + "/correspondances/sync")
				.with(authentication(tokenFor(owner.getUsername()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].ueCode").value("IPAP"))
				.andExpect(jsonPath("$[0].coursReussisIds[0]").value(cours.getId()));

		List<DemandeUE> demandes = dossierService.getDemandesUE(dossier.getId());
		assertEquals(1, demandes.size());
		assertEquals("IPAP", demandes.get(0).getUe().getCode());
		assertEquals(1, demandes.get(0).getCoursReussisJustificatifs().size());
		assertTrue(demandes.get(0).getCoursReussisJustificatifs().stream()
				.anyMatch(c -> c.getId().equals(cours.getId())));
	}

	private Dossier createDossierSansSection(String label) throws DossierException {
		User user = createDedicatedUser(label);
		return dossierService.createDossier(user, "Demande " + label);
	}

	private Dossier createDossierAvecSection(String label, String sectionCode) throws DossierException {
		User user = createDedicatedUser(label);
		return dossierService.createDossier(user, "Demande " + label, getSection(sectionCode));
	}

	private User createDedicatedUser(String label) {
		String suffix = uniqueSuffix();
		String username = "p1_" + label + "_" + suffix;
		User user = new User(username, "p1-" + suffix + "@example.test", "NomTest", "PrenomTest");
		return userDao.saveAndFlush(user);
	}

	private Section getSection(String code) {
		return sectionDao.findById(code)
				.orElseThrow(() -> new AssertionError("Section de test absente: " + code));
	}

	private UE getUe(String code) {
		return ueDao.findById(code)
				.orElseThrow(() -> new AssertionError("UE de test absente: " + code));
	}

	private CoursReussi addValidCourse(Dossier dossier) throws DossierException {
		String suffix = uniqueSuffix();
		return dossierService.addCoursReussi(dossier, "Etab " + suffix, "COUR" + suffix,
				"Cours " + suffix, 8, "14/20", "Programme " + suffix,
				"https://test.local/programmes/" + suffix);
	}

	private CoursReussi addCourse(Dossier dossier, String etablissementNom, String code, String nom, int ects)
			throws DossierException {
		return dossierService.addCoursReussi(dossier, etablissementNom, code, nom, ects, "14/20",
				"Programme " + code, "https://test.local/programmes/" + code);
	}

	private CoursReussi addKnownCourseWithoutProgramSupport(Dossier dossier) throws DossierException {
		String suffix = uniqueSuffix();
		String etablissementNom = "EtabNoPrgm" + suffix;
		Etablissement etablissement = etablissementDao.saveAndFlush(Etablissement.builder()
				.nom(etablissementNom)
				.statut(StatutEtablissement.VALIDE)
				.build());
		coursExterneDao.saveAndFlush(CoursExterne.builder()
				.etablissement(etablissement)
				.code("KNOWN" + suffix)
				.nom("Cours connu sans support " + suffix)
				.ects(5)
				.prgm(null)
				.lienProgramme(null)
				.build());
		return dossierService.addCoursReussi(dossier, etablissementNom, "KNOWN" + suffix,
				"Cours connu sans support " + suffix, 5, "14/20", null, null);
	}

	private DemandeUE addIpapDemande(Dossier dossier) throws DossierException {
		return dossierService.addDemandeUE(dossier, getUe("IPAP"), "Demande de dispense IPAP");
	}

	private DemandeUE addIpidDemande(Dossier dossier) throws DossierException {
		return dossierService.addDemandeUE(dossier, getUe("IPID"), "Demande de dispense IPID");
	}

	private Document addRequiredDocuments(Dossier dossier) {
		addLettreMotivation(dossier);
		return addBulletin(dossier);
	}

	private Document addLettreMotivation(Dossier dossier) {
		String suffix = uniqueSuffix();
		return dossierService.addDocument(dossier, "doc-" + suffix + ".pdf",
				TypeDocument.LETTRE_MOTIVATION, "C:/tmp/doc-" + suffix + ".pdf",
				"http://localhost/files/doc-" + suffix + ".pdf", null);
	}

	private Document addBulletin(Dossier dossier) {
		String suffix = uniqueSuffix();
		return dossierService.addDocument(dossier, "doc-" + suffix + ".pdf",
				TypeDocument.BULLETIN, "C:/tmp/doc-" + suffix + ".pdf",
				"http://localhost/files/doc-" + suffix + ".pdf", null);
	}

	private Document addBulletin(Dossier dossier, CoursReussi cours) {
		String suffix = uniqueSuffix();
		return dossierService.addDocument(dossier, "doc-bulletin-" + suffix + ".pdf",
				TypeDocument.BULLETIN, "C:/tmp/doc-bulletin-" + suffix + ".pdf",
				"http://localhost/files/doc-bulletin-" + suffix + ".pdf", cours);
	}

	private Document addDossierPedagogique(Dossier dossier, CoursReussi cours) {
		String suffix = uniqueSuffix();
		return dossierService.addDocument(dossier, "doc-prgm-" + suffix + ".pdf",
				TypeDocument.DOSSIER_PEDAGOGIQUE, "C:/tmp/doc-prgm-" + suffix + ".pdf",
				"http://localhost/files/doc-prgm-" + suffix + ".pdf", cours);
	}

	private String pedagogicalSupportError(CoursReussi cours) {
		return "Le cours " + cours.getCode() + " doit avoir un dossier pedagogique ou une URL programme";
	}

	private String bulletinError(CoursReussi cours) {
		return "Le cours " + cours.getCode() + " doit avoir un bulletin global ou un bulletin attache au cours";
	}

	private Correspondance createCorrespondance(CoursReussi coursReussi, UE ue, TypeCorrespondance type, String commentaire) {
		return createCorrespondance(List.of(coursReussi), ue, type, commentaire);
	}

	private Correspondance createCorrespondance(List<CoursReussi> coursReussis, UE ue, TypeCorrespondance type,
			String commentaire) {
		HashSet<org.isfce.pid.model.CoursExterne> coursExternes = new HashSet<>();
		for (CoursReussi coursReussi : coursReussis) {
			coursExternes.add(coursReussi.getCoursExterne());
		}
		Correspondance correspondance = Correspondance.builder()
				.etablissement(coursReussis.get(0).getEtablissement())
				.ue(ue)
				.typeCorrespondance(type)
				.commentaire(commentaire)
				.coursExternes(coursExternes)
				.build();
		return correspondanceDao.saveAndFlush(correspondance);
	}

	private void assertValidationErrors(Dossier dossier, String... expectedErrors) {
		var validation = dossierService.validateDossier(dossier.getId());
		assertFalse(validation.complet());
		assertEquals(expectedErrors.length, validation.erreurs().size());
		for (String expectedError : expectedErrors) {
			assertTrue(validation.erreurs().contains(expectedError));
		}
	}

	private void assertSubmissionRefused(Dossier dossier, String... expectedErrors) {
		DossierException exc = assertThrows(DossierException.class,
				() -> dossierService.soumettreDossier(dossier.getId()));
		assertTrue(exc.getMessage().startsWith("Dossier incomplet: "));
		for (String expectedError : expectedErrors) {
			assertTrue(exc.getMessage().contains(expectedError));
		}
	}

	private JwtAuthenticationToken tokenFor(String username) {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", username)
				.claim("preferred_username", username).build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ETUDIANT");
		return new JwtAuthenticationToken(jwt, authorities);
	}

	private String uniqueSuffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
