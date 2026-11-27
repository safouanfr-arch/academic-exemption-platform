package org.isfce.pid.service;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.dao.SectionDao;
import org.isfce.pid.dao.UeDao;
import org.isfce.pid.dto.SectionDto;
import org.isfce.pid.dto.UEDto;
import org.isfce.pid.dto.UEFullDto;
import org.isfce.pid.mapper.UEMapper;
import org.isfce.pid.model.UE;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UEService {

	private final UeDao ueDao;
	private final SectionDao sectionDao;
	private final UEMapper ueMapper;

	/**
	 * Retour brut des entites UE.
	 * Utile surtout pour les besoins internes ou certains tests.
	 */
	public List<UE> findAllUeEntities() {
		return ueDao.findAll();
	}

	/**
	 * Retour detaille d'une UE avec ses acquis.
	 */
	public Optional<UEFullDto> findUeDetail(String ueCode) {
		return ueDao.findById(ueCode).map(ueMapper::toUeDetailDto);
	}

	/**
	 * Cree une nouvelle UE depuis le DTO complet envoye par l'admin.
	 */
	public UEFullDto createUe(@Valid UEFullDto ueDetailDto) {
		UE savedUe = ueDao.save(ueMapper.fromUeDetailDto(ueDetailDto));
		return ueMapper.toUeDetailDto(savedUe);
	}

	public boolean ueExists(String ueCode) {
		return ueDao.existsById(ueCode);
	}

	public void deleteUe(String ueCode) {
		ueDao.deleteById(ueCode);
	}

	/**
	 * Liste "legere" des UE, sans les acquis.
	 * C'est celle utilisee pour afficher les catalogues/lists.
	 */
	public List<UEDto> getUeSummaries() {
		return ueDao.findAllUE_Dto();
	}

	/**
	 * Meme idee, mais filtree par section.
	 */
	public List<UEDto> getUeSummariesBySection(String sectionCode) {
		return ueDao.findAllUEBySection_Dto(sectionCode);
	}

	/**
	 * Les sections changent peu, donc on les met en cache.
	 */
	@Cacheable
	public List<SectionDto> getSectionSummaries() {
		return sectionDao.findAllSection_Dto();
	}
}
