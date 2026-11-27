package org.isfce.pid.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.isfce.pid.dao.ICoursExterneDao;
import org.isfce.pid.dao.IEtablissementDao;
import org.isfce.pid.dto.CoursExterneReferenceDto;
import org.isfce.pid.dto.EtablissementReferenceDto;
import org.isfce.pid.model.CoursExterne;
import org.isfce.pid.model.Etablissement;
import org.isfce.pid.model.Etablissement.StatutEtablissement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/references", produces = "application/json")
@CrossOrigin("*")
public class ReferenceController {

	private final IEtablissementDao etablissementDao;
	private final ICoursExterneDao coursExterneDao;

	public ReferenceController(IEtablissementDao etablissementDao, ICoursExterneDao coursExterneDao) {
		this.etablissementDao = etablissementDao;
		this.coursExterneDao = coursExterneDao;
	}

	/**
	 * On ne renvoie ici que les ecoles validees dans la base de reference.
	 */
	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("/etablissements")
	public ResponseEntity<List<EtablissementReferenceDto>> listerEtablissementsConnus() {
		List<EtablissementReferenceDto> etablissements =
				etablissementDao.findByStatutOrderByNomAsc(StatutEtablissement.VALIDE)
				.stream()
				.map(this::versDtoEtablissement)
				.toList();

		return ResponseEntity.ok(etablissements);
	}

	/**
	 * Apres le choix de l'ecole, on propose les cours connus de cette ecole.
	 */
	@PreAuthorize("hasRole('ETUDIANT')")
	@GetMapping("/etablissements/{id}/cours")
	public ResponseEntity<List<CoursExterneReferenceDto>> listerCoursConnusPourEtablissement(
			@PathVariable("id") Long etablissementId) {
		etablissementDao.findById(etablissementId)
				.orElseThrow(() -> new NoSuchElementException("Etablissement non trouve: " + etablissementId));

		List<CoursExterneReferenceDto> cours = coursExterneDao.findByEtablissementIdOrderByCodeAsc(etablissementId)
				.stream()
				.map(coursExterne -> versDtoCours(coursExterne, etablissementId))
				.toList();

		return ResponseEntity.ok(cours);
	}

	private EtablissementReferenceDto versDtoEtablissement(Etablissement etablissement) {
		return new EtablissementReferenceDto(
				etablissement.getId(),
				etablissement.getNom(),
				etablissement.getSiteWeb(),
				etablissement.getStatut());
	}

	private CoursExterneReferenceDto versDtoCours(CoursExterne cours, Long etablissementId) {
		return new CoursExterneReferenceDto(
				cours.getId(),
				etablissementId,
				cours.getCode(),
				cours.getNom(),
				cours.getEcts(),
				cours.getPrgm(),
				cours.getLienProgramme());
	}
}
