package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rapprochement_bancaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapprochementBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_financier_id", nullable = false)
    private CompteFinancier compteFinancier;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(precision = 18, scale = 2)
    private BigDecimal soldeInitialReleve;

    @Column(precision = 18, scale = 2)
    private BigDecimal soldeFinalReleve;

    private LocalDateTime dateCreation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    private boolean valide = false;
    private LocalDateTime dateValidation;
}
