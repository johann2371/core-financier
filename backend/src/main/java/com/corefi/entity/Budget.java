package com.corefi.entity;

import com.corefi.enums.CategorieDecaissement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "budget")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategorieDecaissement categorie;

    private int annee;
    private int mois; // 0 = annuel, 1-12 = mensuel

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPlafond = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantConsomme = BigDecimal.ZERO;

    private boolean alerteEnvoyee = false;

    /** Seuil d'alerte en pourcentage (ex: 80 = alerte à 80% du budget) */
    private int seuilAlertePourcent = 80;
}
