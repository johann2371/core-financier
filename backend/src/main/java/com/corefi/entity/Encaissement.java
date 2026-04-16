package com.corefi.entity;

import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutEncaissement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "encaissement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Encaissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    @Column(nullable = false)
    private LocalDate dateEncaissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Tiers client;

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

    private String numeroRecu;
    private String reference;

    // --- Métadonnées Paiement (Chèque, Virement, Mobile Money) ---
    private String banqueEmettrice;
    private String numeroOperation;
    private LocalDate dateOperation;
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutEncaissement statut = StatutEncaissement.VALIDEE;

    // --- Frais et Compensation ---
    @Column(precision = 18, scale = 2)
    private BigDecimal fraisTransaction;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantNet;

    private LocalDate datePrevisionnelleCompensation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saisi_par")
    private Utilisateur saisiPar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    private boolean rapproche = false;
    private LocalDate dateRapprochement;
}
