package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "justificatif_decaissement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifDecaissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom du fichier stocké sur le serveur (UUID + extension) */
    @Column(nullable = false)
    private String nomFichier;

    /** Nom original du fichier uploadé par l'utilisateur */
    @Column(nullable = false)
    private String nomOriginal;

    /** Type MIME du fichier (application/pdf, image/png, etc.) */
    private String typeFichier;

    /** Taille du fichier en octets */
    private Long tailleFichier;

    /** Date d'upload */
    private LocalDateTime dateUpload = LocalDateTime.now();

    /** Décaissement associé */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decaissement_id", nullable = false)
    private Decaissement decaissement;
}
