package org.isfce.pid.dao;

import java.util.List;

import org.isfce.pid.model.DemandeUE;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.UE;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDemandeUEDao extends JpaRepository<DemandeUE, Long> {

    List<DemandeUE> findByDossier(Dossier dossier);

    List<DemandeUE> findByDossierId(Long dossierId);

    boolean existsByDossierAndUe(Dossier dossier, UE ue);

    boolean existsByDossierIdAndUeCode(Long dossierId, String ueCode);
}
