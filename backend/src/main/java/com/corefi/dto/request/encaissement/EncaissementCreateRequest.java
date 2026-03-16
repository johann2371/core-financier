package com.corefi.dto.request.encaissement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EncaissementCreateRequest {
    @NotNull
    private Long clientId;

    @NotNull
    @Positive
    private BigDecimal montant;

    @NotBlank
    private String moyenPaiement; // ESPECES, CHEQUE, etc.

    @NotNull
    private Long compteFinancierId;

    private Long deviseId;
    private String reference;

    private List<AffectationRequest> affectations;
}
