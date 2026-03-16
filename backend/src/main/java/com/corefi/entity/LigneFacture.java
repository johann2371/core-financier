package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "ligne_facture")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    private Integer numeroLigne;

    @Column(nullable = false)
    private String designation;

    @Column(precision = 18, scale = 4)
    private BigDecimal quantite;

    private String unite;

    @Column(precision = 18, scale = 2)
    private BigDecimal prixUnitaire;

    // TVA camerounaise : 19,25%
    @Column(precision = 5, scale = 2)
    private BigDecimal tauxTva = new BigDecimal("19.25");

    @Column(precision = 18, scale = 2)
    private BigDecimal montantHt;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantTva;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantTtc;
}
