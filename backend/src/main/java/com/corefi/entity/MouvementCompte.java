package com.corefi.entity;

import com.corefi.enums.TypeMouvement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvement_compte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_financier_id", nullable = false)
    private CompteFinancier compteFinancier;

    private LocalDateTime dateOperation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvement typeMouvement;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantDebit;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantCredit;

    @Column(precision = 18, scale = 2)
    private BigDecimal soldeApres;

    private Long transactionId;
    private String transactionType; // ENCAISSEMENT ou DECAISSEMENT
}
