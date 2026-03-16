package com.corefi.dto.request.encaissement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AffectationRequest {
    @NotNull
    private Long factureId;

    @NotNull
    @Positive
    private BigDecimal montantAffecte;
}
