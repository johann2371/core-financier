package com.corefi.entity;

import com.corefi.enums.TypeCompte;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "compte_financier")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteFinancier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCompte type;

    @Column(unique = true, nullable = false)
    private String numero;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_id")
    private Devise devise;

    private boolean actif = true;
}
