package com.corefi.dto.request.facture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LigneFactureRequest {
    @NotBlank
    private String designation;

    @NotNull
    @Positive
    private BigDecimal quantite;

    @NotNull
    @Positive
    private BigDecimal prixUnitaire;
}
