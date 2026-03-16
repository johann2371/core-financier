package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fournisseur")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Fournisseur extends Tiers {

    private String numeroCompte;
    private String iban;
    private Integer delaiPaiementMoyen; // en jours
}
