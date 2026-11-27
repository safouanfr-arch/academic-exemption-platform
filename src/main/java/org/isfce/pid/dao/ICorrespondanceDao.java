package org.isfce.pid.dao;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.model.Correspondance;
import org.isfce.pid.model.Etablissement;
import org.isfce.pid.model.UE;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICorrespondanceDao extends JpaRepository<Correspondance, Long> {

    List<Correspondance> findByUe(UE ue);

    List<Correspondance> findByEtablissement(Etablissement etablissement);

    Optional<Correspondance> findByEtablissementAndUe(Etablissement etablissement, UE ue);

    boolean existsByEtablissementAndUe(Etablissement etablissement, UE ue);
}
