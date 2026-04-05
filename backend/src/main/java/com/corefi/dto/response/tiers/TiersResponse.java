package com.corefi.dto.response.tiers;

import lombok.Data;
import java.math.BigDecimal;

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
    private BigDecimal solde;
    private BigDecimal totalDette;
    private String nui;
    private String rccm;
    private boolean actif;

    // Champs spécifiques CLIENT
    private String typeClient;
    private String cni;
    private String photoUrl;
    private Double creditLimite;
    private Integer delaiPaiement;

    // Champs spécifiques FOURNISSEUR
    private String numeroCompte;
    private String iban;
}
