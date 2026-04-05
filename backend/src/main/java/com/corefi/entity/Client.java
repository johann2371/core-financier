package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "client")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Client extends Tiers {

    private String typeClient; // PARTICULIER ou ENTREPRISE
    private String cni; // Carte Nationale d'Identité
    private String photoUrl;

    private BigDecimal creditLimite;

    private Integer delaiPaiement; // en jours
}
