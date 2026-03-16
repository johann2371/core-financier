package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "taux_change")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TauxChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_source_id", nullable = false)
    private Devise deviseSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_cible_id", nullable = false)
    private Devise deviseCible;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal taux;

    private LocalDate dateDebut;
    private boolean actif = true;
}
