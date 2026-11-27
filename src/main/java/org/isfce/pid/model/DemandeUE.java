package org.isfce.pid.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "TDEMANDE_UE")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"FKDOSSIER", "FKUE"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class DemandeUE {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FKDOSSIER", nullable = false)
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "FKUE", nullable = false)
    private UE ue;

    @Setter
    @Column(length = 500)
    private String commentaire;

    @Setter
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "TDEMANDE_UE_COURS",
        joinColumns = @JoinColumn(name = "FKDEMANDEUE"),
        inverseJoinColumns = @JoinColumn(name = "FKCOURSREUSSI")
    )
    private Set<CoursReussi> coursReussisJustificatifs = new HashSet<>();
}
