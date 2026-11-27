package org.isfce.pid.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity(name = "TCORRESPONDANCE")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"FKETABLISSEMENT", "FKUE"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Correspondance {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FKETABLISSEMENT", nullable = false)
    private Etablissement etablissement;

    @ManyToOne
    @JoinColumn(name = "FKUE", nullable = false)
    private UE ue;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TypeCorrespondance typeCorrespondance;

    @Setter
    @Column(length = 500)
    private String commentaire;

    @Setter
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "TCORRESPONDANCE_COURS",
        joinColumns = @JoinColumn(name = "FKCORRESPONDANCE"),
        inverseJoinColumns = @JoinColumn(name = "FKCOURSEXT")
    )
    private Set<CoursExterne> coursExternes = new HashSet<>();
}
