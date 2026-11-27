package org.isfce.pid.dossier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.isfce.pid.controller.error.DossierException;
import org.isfce.pid.dao.IEtablissementDao;
import org.isfce.pid.dao.IUserDao;
import org.isfce.pid.dao.SectionDao;
import org.isfce.pid.model.CoursReussi;
import org.isfce.pid.model.DemandeUE;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.Section;
import org.isfce.pid.model.User;
import org.isfce.pid.service.DossierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("testU")
@Sql(scripts = {"/dataTestU.sql", "/correspondances-seed.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class TestSeedCorrespondancesData {

	@Autowired
	private DossierService dossierService;

	@Autowired
	private IEtablissementDao etablissementDao;

	@Autowired
	private IUserDao userDao;

	@Autowired
	private SectionDao sectionDao;

	@Test
	void seedContientLesCinqEtablissementsAttendus() {
		for (String nom : List.of("Vinci", "ULB", "HELB", "HE2B", "EPHEC")) {
			assertTrue(etablissementDao.findByNomIgnoreCase(nom).isPresent(), "Etablissement seed manquant: " + nom);
		}
	}

	@Test
	void correspondanceSeedVinciSimple_estDetecteeEtSynchronisee() throws Exception {
		Dossier dossier = createDossierAvecSection("seed_vinci_simple");
		CoursReussi cours = addCoursSeed(dossier, "Vinci", "VIN-IPAP-101", "Algorithmique et programmation", 8);

		List<DossierService.CorrespondanceConnue> suggestions = dossierService.getCorrespondancesConnues(dossier);
		assertEquals(1, suggestions.size());
		assertEquals("IPAP", suggestions.get(0).correspondance().getUe().getCode());
		assertEquals(cours.getId(), suggestions.get(0).coursReussi().getId());

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);
		assertEquals(1, synced.size());
		assertEquals("IPAP", synced.get(0).getUe().getCode());
		assertEquals(1, synced.get(0).getCoursReussisJustificatifs().size());
	}

	@Test
	void correspondanceSeedUlbCombinee_exigeTousLesCoursEtSynchroniseLesDeux() throws Exception {
		Dossier dossier = createDossierAvecSection("seed_ulb_combine");
		addCoursSeed(dossier, "ULB", "ULB-IPAP-201", "Algorithmique", 4);

		List<DossierService.CorrespondanceConnue> suggestionsIncompletes = dossierService.getCorrespondancesConnues(dossier);
		assertTrue(suggestionsIncompletes.isEmpty());

		addCoursSeed(dossier, "ULB", "ULB-IPAP-202", "Programmation", 4);

		List<DossierService.CorrespondanceConnue> suggestionsCompletes = dossierService.getCorrespondancesConnues(dossier);
		assertEquals(2, suggestionsCompletes.size());
		assertEquals(
				Set.of("ULB-IPAP-201", "ULB-IPAP-202"),
				suggestionsCompletes.stream().map(s -> s.coursReussi().getCode()).collect(Collectors.toSet()));

		List<DemandeUE> synced = dossierService.syncCorrespondancesConnues(dossier);
		assertEquals(1, synced.size());
		assertEquals("IPAP", synced.get(0).getUe().getCode());
		assertEquals(2, synced.get(0).getCoursReussisJustificatifs().size());
		assertEquals(
				Set.of("ULB-IPAP-201", "ULB-IPAP-202"),
				synced.get(0).getCoursReussisJustificatifs().stream().map(CoursReussi::getCode).collect(Collectors.toSet()));
	}

	private Dossier createDossierAvecSection(String label) throws DossierException {
		User user = createDedicatedUser(label);
		Section section = sectionDao.findById("INFO")
				.orElseThrow(() -> new AssertionError("Section INFO absente"));
		return dossierService.createDossier(user, "Demande " + label, section);
	}

	private User createDedicatedUser(String label) {
		String username = "seed_" + label + "_" + UUID.randomUUID().toString().substring(0, 8);
		String emailLocalPart = username.length() > 40 ? username.substring(0, 40) : username;
		User user = new User(username, emailLocalPart + "@example.test", "NomSeed", "PrenomSeed");
		return userDao.saveAndFlush(user);
	}

	private CoursReussi addCoursSeed(Dossier dossier, String etablissementNom, String code, String nom, int ects)
			throws DossierException {
		return dossierService.addCoursReussi(dossier, etablissementNom, code, nom, ects, "14/20", null, null);
	}
}
