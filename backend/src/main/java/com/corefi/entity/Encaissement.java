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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEncaissement statut = StatutEncaissement.VALIDEE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saisi_par")
    private Utilisateur saisiPar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;
}
