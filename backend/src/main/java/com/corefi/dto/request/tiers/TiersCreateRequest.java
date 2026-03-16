package com.corefi.dto.request.tiers;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TiersCreateRequest {
    @NotBlank
    private String type; // CLIENT ou FOURNISSEUR

    @NotBlank
    private String raisonSociale;

    private String telephone;
    private String email;
    private String adresse;
}
