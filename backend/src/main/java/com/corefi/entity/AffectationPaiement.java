package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "affectation_paiement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AffectationPaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;
    private String transactionType; // ENCAISSEMENT ou DECAISSEMENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montantAffecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affecte_par")
    private Utilisateur affectePar;
}
