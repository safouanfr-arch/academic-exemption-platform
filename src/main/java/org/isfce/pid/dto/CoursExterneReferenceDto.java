package org.isfce.pid.dto;

public record CoursExterneReferenceDto(
		Long id,
		Long etablissementId,
		String code,
		String nom,
		int ects,
		String prgm,
		String lienProgramme) {
}
