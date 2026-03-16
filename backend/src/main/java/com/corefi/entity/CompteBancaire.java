package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "compte_bancaire")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CompteBancaire extends CompteFinancier {

    private String banque;
    private String agence;
    private String iban;
    private String bic;
    private String typeCompte; // ex: COURANT, EPARGNE
}
