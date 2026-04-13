package com.corefi.entity;

import com.corefi.enums.StatutSessionCaisse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_caisse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_id", nullable = false)
    private Utilisateur caissier;

    @Column(nullable = false)
    private LocalDateTime dateOuverture;

    private LocalDateTime dateFermeture;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeInitial;

    @Column(precision = 18, scale = 2)
    private BigDecimal soldeFinalTheorique;

    @Column(precision = 18, scale = 2)
    private BigDecimal soldeFinalReel;

    @Column(precision = 18, scale = 2)
    private BigDecimal ecart;

    @Column(columnDefinition = "TEXT")
    private String motifEcart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSessionCaisse statut = StatutSessionCaisse.OUVERTE;
}
