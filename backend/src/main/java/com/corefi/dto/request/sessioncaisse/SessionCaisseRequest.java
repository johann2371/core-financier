package com.corefi.dto.request.sessioncaisse;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SessionCaisseRequest {
    @NotNull(message = "L'ID de la caisse est obligatoire")
    private Long caisseId;
    
    @NotNull(message = "Le solde initial est obligatoire")
    private BigDecimal soldeInitial;
}
