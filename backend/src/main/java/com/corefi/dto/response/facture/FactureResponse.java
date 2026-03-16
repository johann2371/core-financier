package com.corefi.dto.response.facture;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FactureResponse {
    private Long id;
    private String numero;
    private String type;
    private LocalDate dateFacture;
    private String tiersNom;
    private BigDecimal montantTtc;
    private String statut;
}
