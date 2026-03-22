package com.corefi.dto.request.decaissement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

import java.time.LocalDate;

@Data
public class DecaissementCreateRequest {
    @NotNull
    private Long fournisseurId;

    @NotNull
    @Positive
    private BigDecimal montant;

    @NotBlank
    private String beneficiaire;

    private Long deviseId;
    private String motif;

    @NotBlank
    private String moyenPaiement;

    private String banqueEmettrice;
    private String numeroOperation;
    private LocalDate dateOperation;
    private String telephone;
}
