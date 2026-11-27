package org.isfce.pid.controller;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.isfce.pid.controller.error.DuplicateException;
import org.isfce.pid.dto.SectionDto;
import org.isfce.pid.dto.UEDto;
import org.isfce.pid.dto.UEFullDto;
import org.isfce.pid.service.UEService;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/ue/", produces = "application/json")
@CrossOrigin("*")
public class UEControllerRest {

	private final UEService ueService;
	private final MessageSource messageSource;

	public UEControllerRest(UEService ueService, MessageSource messageSource) {
		this.ueService = ueService;
		this.messageSource = messageSource;
	}

	@GetMapping("sections")
	ResponseEntity<List<SectionDto>> listerSections() {
		return ResponseEntity.ok(ueService.getSectionSummaries());
	}

	@GetMapping("liste")
	ResponseEntity<List<UEDto>> listerUe(
			@RequestParam(name = "section", required = false, defaultValue = "all") String codeSection) {
		if ("all".equals(codeSection)) {
			return ResponseEntity.ok(ueService.getUeSummaries());
		}
		return ResponseEntity.ok(ueService.getUeSummariesBySection(codeSection));
	}

	@GetMapping("/detail/{code}")
	ResponseEntity<UEFullDto> consulterDetailUe(@PathVariable(name = "code") String codeUe, Locale locale) {
		if (!ueService.ueExists(codeUe)) {
			throw new NoSuchElementException(
					messageSource.getMessage("err.notFound", new String[] {codeUe}, locale));
		}

		return ResponseEntity.ok(ueService.findUeDetail(codeUe).orElseThrow());
	}

	/**
	 * Route admin : ajoute une UE et ses acquis.
	 */
	@PostMapping(path = "add", consumes = "application/json")
	ResponseEntity<UEFullDto> creerUe(@Valid @RequestBody UEFullDto ueDetailDto, Locale locale) {
		if (ueService.ueExists(ueDetailDto.getCode())) {
			throw new DuplicateException(
					messageSource.getMessage("err.doublon", new String[] {"UE"}, locale),
					ueDetailDto.getCode());
		}

		UEFullDto ueEnregistree = ueService.createUe(ueDetailDto);
		return new ResponseEntity<>(ueEnregistree, HttpStatus.CREATED);
	}

	@DeleteMapping(path = "{code}/delete")
	ResponseEntity<String> supprimerUe(@PathVariable("code") String codeUe) {
		if (!ueService.ueExists(codeUe)) {
			return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
		}

		ueService.deleteUe(codeUe);
		return ResponseEntity.ok(codeUe);
	}
}
