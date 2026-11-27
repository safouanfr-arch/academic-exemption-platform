package org.isfce.pid.controller.error;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MonAdviceRestController {

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<String> gererElementIntrouvable(NoSuchElementException exception) {
		return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
	}

	/**
	 * Quand une regle "unicite" casse, on renvoie le champ concerne + son message.
	 */
	@ExceptionHandler(DuplicateException.class)
	public ResponseEntity<Map<String, String>> gererDoublon(DuplicateException exception) {
		return ResponseEntity.badRequest().body(Map.of(exception.getChamp(), exception.getMessage()));
	}

	/**
	 * Les erreurs dossier sont renvoyees en 400 avec une structure simple pour l'interface.
	 */
	@ExceptionHandler(DossierException.class)
	public ResponseEntity<Map<String, String>> gererErreurDossier(DossierException exception) {
		return ResponseEntity.badRequest().body(Map.of("objetDemande", exception.getMessage()));
	}

	/**
	 * Spring collecte deja les erreurs de validation de formulaire.
	 * Ici on les transforme juste en map champ -> message.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> gererErreursValidation(MethodArgumentNotValidException exception) {
		Map<String, String> erreursParChamp = new HashMap<>();

		exception.getBindingResult().getFieldErrors()
				.forEach(erreur -> erreursParChamp.put(erreur.getField(), erreur.getDefaultMessage()));

		return ResponseEntity.badRequest().body(erreursParChamp);
	}
}
