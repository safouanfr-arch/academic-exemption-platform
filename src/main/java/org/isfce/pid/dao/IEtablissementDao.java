package org.isfce.pid.dao;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.model.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEtablissementDao extends JpaRepository<Etablissement, Long> {

    Optional<Etablissement> findByNomIgnoreCase(String nom);

    List<Etablissement> findByStatutOrderByNomAsc(Etablissement.StatutEtablissement statut);
}
