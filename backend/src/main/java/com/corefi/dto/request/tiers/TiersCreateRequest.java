package com.corefi.dto.request.tiers;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TiersCreateRequest {

    @NotBlank(message = "Le type est requis (CLIENT ou FOURNISSEUR)")
    private String type; // CLIENT ou FOURNISSEUR

    @NotBlank(message = "La raison sociale est requise")
    private String raisonSociale;

    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String pays;

    // Champs spécifiques CLIENT
    private String typeClient;      // PARTICULIER ou ENTREPRISE
    private Double creditLimite;
    private Integer delaiPaiement;

    // Champs spécifiques FOURNISSEUR
    private String numeroCompte;
    private String iban;
}
