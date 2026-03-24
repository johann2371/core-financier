package com.corefi.entity;

import com.corefi.enums.StatutFacture;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "facture")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // VENTE ou ACHAT

    @Column(unique = true, nullable = false)
    private String numero;

    private LocalDate dateFacture;
    private LocalDate dateEcheance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tiers_id", nullable = false)
    private Tiers tiers;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantTtc = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutFacture statut = StatutFacture.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_id")
    private Devise devise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFacture> lignes;
}
