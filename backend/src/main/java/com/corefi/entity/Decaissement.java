package com.corefi.entity;

import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.CategorieDecaissement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decaissement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Decaissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    private LocalDate dateDecaissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Tiers fournisseur;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_id")
    private Devise devise;

    @Column(precision = 18, scale = 6)
    private BigDecimal tauxChange;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyenPaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_financier_id")
    private CompteFinancier compteFinancier;

    private String beneficiaire;
    private String reference;
    private String motif; // Raison de la demande de décaissement

    // --- Catégorie de décaissement ---
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CategorieDecaissement categorie = CategorieDecaissement.PAIEMENT_FOURNISSEUR;

    // --- Justificatifs joints ---
    @OneToMany(mappedBy = "decaissement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JustificatifDecaissement> justificatifs = new ArrayList<>();

    // --- Checklist de vérification RF (stocké en JSON) ---
    @Column(columnDefinition = "TEXT")
    private String checklistRF;

    // --- Métadonnées Paiement (Chèque, Virement, Mobile Money) ---
    private String banqueEmettrice;
    private String numeroOperation;
    private LocalDate dateOperation;
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutDecaissement statut = StatutDecaissement.BROUILLON;

    // --- Saisie ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saisi_par")
    private Utilisateur saisiPar;

    private LocalDateTime dateSaisie;

    // --- Validation RF ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    private LocalDateTime dateValidation;
    private String motifRejetRf;

    // --- Approbation PDG ---
    private boolean seuilPdgRequis = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approuve_par_pdg")
    private Utilisateur approuveParPdg;

    private LocalDateTime dateApprobationPdg;
    private String motifRejetPdg;

    // --- Exécution Caissier ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execute_par")
    private Utilisateur executePar;

    private LocalDateTime dateExecution;
    private String referenceExecution;
}
