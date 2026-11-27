package org.isfce.pid.dao;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.model.CoursExterne;
import org.isfce.pid.model.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICoursExterneDao extends JpaRepository<CoursExterne, Long> {

    Optional<CoursExterne> findByEtablissementAndCodeIgnoreCase(Etablissement etablissement, String code);

    List<CoursExterne> findByEtablissementIdOrderByCodeAsc(Long etablissementId);
}
