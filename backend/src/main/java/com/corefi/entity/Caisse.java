package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "caisse")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Caisse extends CompteFinancier {

    private String emplacement;

    private BigDecimal soldeLimite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;
}
