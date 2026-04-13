package com.corefi.dto.request.sessioncaisse;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SessionCaisseFermetureRequest {
    @NotNull(message = "Le solde réel est obligatoire")
    private BigDecimal soldeFinalReel;
    
    private String motifEcart;
}
