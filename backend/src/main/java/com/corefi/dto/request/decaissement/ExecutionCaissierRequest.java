package com.corefi.dto.request.decaissement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExecutionCaissierRequest {
    @NotNull
    private Long compteFinancierId;

    @NotBlank
    private String moyenPaiement;

    private String referenceExecution;
}
