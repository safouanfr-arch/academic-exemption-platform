package org.isfce.pid.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "TDOCUMENT")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Document {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FKDOSSIER", nullable = false)
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "FKCOURSREUSSI")
    private CoursReussi coursReussi;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "typeDocument", length = 30, nullable = false)
    private TypeDocument typeDocument;

    @NotBlank
    @Column(length = 200, nullable = false)
    private String nomFichier;

    @Setter
    @Column(length = 500)
    private String cheminFichier;

    // Opaque filename assigned only to files stored by the server.
    @Column(length = 36)
    private String cleStockage;

    @Setter
    @Column(length = 500)
    private String urlDocument;
}
