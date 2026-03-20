package com.corefi.dto.response.tiers;

import lombok.Data;

@Data
public class TiersResponse {
    private Long id;
    private String code;
    private String type;           // CLIENT ou FOURNISSEUR
    private String raisonSociale;
    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String pays;
    private boolean actif;
    private Double solde;

    // Champs spécifiques CLIENT
    private String typeClient;
    private Double creditLimite;
    private Integer delaiPaiement;

    // Champs spécifiques FOURNISSEUR
    private String numeroCompte;
    private String iban;
}
