package com.corefi.dto.request.encaissement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    private String banqueEmettrice;
    private String numeroOperation;
    private LocalDate dateOperation;
    private String telephone;

    private BigDecimal fraisTransaction;
    private LocalDate datePrevisionnelleCompensation;

    private List<AffectationRequest> affectations;
}
