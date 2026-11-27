package org.isfce.pid.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.isfce.pid.dto.AcquisDto;
import org.isfce.pid.dto.AcquisFullDto;
import org.isfce.pid.dto.DossierDto;
import org.isfce.pid.dto.SectionDto;
import org.isfce.pid.dto.UEDto;
import org.isfce.pid.dto.UEFullDto;
import org.isfce.pid.model.Acquis;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.Section;
import org.isfce.pid.model.UE;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Ce mapper sert de traducteur entre :
 * - les entites JPA cote serveur
 * - les DTO exposes par l'API
 */
@Mapper(componentModel = "spring")
public interface UEMapper {

	DossierDto toDossierDto(Dossier dossier);

	SectionDto toSectionDto(Section section);

	// ==================== UE ====================

	/**
	 * Version "resume" d'une UE : pas les acquis, juste les infos generales.
	 */
	UEDto toUeSummaryDto(UE ue);

	List<UEDto> toUeSummaryDtoList(List<UE> ueList);

	/**
	 * Version detaillee d'une UE : contient aussi la liste des acquis.
	 */
	@Mapping(source = "acquis", target = "acquis")
	UEFullDto toUeDetailDto(UE ue);

	List<UEFullDto> toUeDetailDtoList(List<UE> ueList);

	@Mapping(target = "acquis", ignore = true)
	UE fromUeSummaryDto(UEDto ueDto);

	@Mapping(target = "acquis", expression = "java(mapAcquisListFromDetailDto(ueDto))")
	UE fromUeDetailDto(UEFullDto ueDto);

	// ==================== ACQUIS ====================

	/**
	 * AcquisFullDto n'a pas le code UE dans son id.
	 * On le recompose donc a la main avec le code de l'UE parente.
	 */
	default Acquis toAcquisEntity(AcquisFullDto acquisDto, String ueCode) {
		if (acquisDto == null) {
			return null;
		}
		return new Acquis(
				new Acquis.IdAcquis(ueCode, acquisDto.getNum()),
				acquisDto.getAcquis(),
				acquisDto.getPourcentage());
	}

	default List<Acquis> mapAcquisListFromDetailDto(UEFullDto ueDto) {
		if (ueDto.getAcquis() == null) {
			return List.of();
		}
		return ueDto.getAcquis().stream()
				.map(acquisDto -> toAcquisEntity(acquisDto, ueDto.getCode()))
				.collect(Collectors.toList());
	}

	@Mapping(source = "id.fkUE", target = "fkUE")
	@Mapping(source = "id.num", target = "num")
	AcquisDto toAcquisDto(Acquis acquis);

	@Mapping(target = "id", expression = "java(new Acquis.IdAcquis(acquisDto.getFkUE(), acquisDto.getNum()))")
	Acquis toAcquisEntity(AcquisDto acquisDto);

	default List<Acquis> toAcquisEntityList(List<AcquisDto> acquisDtos) {
		if (acquisDtos == null) {
			return null;
		}
		return acquisDtos.stream()
				.map(this::toAcquisEntity)
				.collect(Collectors.toList());
	}

	List<AcquisDto> toAcquisDtoList(List<Acquis> acquisList);

	@Mapping(source = "id.num", target = "num")
	@Mapping(source = "acquis", target = "acquis")
	@Mapping(source = "pourcentage", target = "pourcentage")
	AcquisFullDto toAcquisFullDto(Acquis acquis);
}
