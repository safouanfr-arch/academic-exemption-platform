package org.isfce.pid.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "TCOURS_REUSSI")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"FKDOSSIER", "FKETABLISSEMENT", "code"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class CoursReussi {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FKDOSSIER", nullable = false)
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "FKETABLISSEMENT")
    private Etablissement etablissement;

    @Column(name = "ETABLISSEMENT_NOM_LIBRE", length = 200)
    private String etablissementNomLibre;

    @ManyToOne
    @JoinColumn(name = "FKCOURSEXT")
    private CoursExterne coursExterne;

    @NotBlank
    @Column(length = 50, nullable = false)
    private String code;

    @NotBlank
    @Column(length = 200, nullable = false)
    private String nom;

    @Min(1)
    @Column(nullable = false)
    private int ects;

    @Setter
    @Column(length = 30)
    private String note;

    @Setter
    @Lob
    private String prgm;

    @Setter
    @Column(length = 500)
    private String lienProgramme;
}
