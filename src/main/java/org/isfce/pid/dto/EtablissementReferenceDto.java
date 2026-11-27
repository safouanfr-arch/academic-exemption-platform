package org.isfce.pid.dto;

import org.isfce.pid.model.Etablissement.StatutEtablissement;

public record EtablissementReferenceDto(
		Long id,
		String nom,
		String siteWeb,
		StatutEtablissement statut) {
}
