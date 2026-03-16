package com.corefi.dto.request.facture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class FactureCreateRequest {
    @NotNull
    private Long tiersId;

    @NotBlank
    private String type; // VENTE ou ACHAT

    @NotEmpty
    private List<LigneFactureRequest> lignes;
}
