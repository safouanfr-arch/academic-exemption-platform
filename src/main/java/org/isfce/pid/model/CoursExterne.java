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

@Entity(name = "TCOURS_EXTERNE")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"FKETABLISSEMENT", "code"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class CoursExterne {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FKETABLISSEMENT", nullable = false)
    private Etablissement etablissement;

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
    @Lob
    private String prgm;

    @Setter
    @Column(length = 500)
    private String lienProgramme;
}
