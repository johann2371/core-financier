package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ligne_releve_bancaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneReleveBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapprochement_id", nullable = false)
    private RapprochementBancaire rapprochement;

    @Column(nullable = false)
    private LocalDate dateOperation;

    @Column(nullable = false)
    private String libelle;

    @Column(precision = 18, scale = 2)
    private BigDecimal debit;

    @Column(precision = 18, scale = 2)
    private BigDecimal credit;

    private boolean matched = false;
    
    // Type d'opération (VIR, CHQ, FRM, etc.)
    private String typeOperation;
    
    // Référence externe éventuelle
    private String referenceBancaire;
}
