package com.corefi.dto.response.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureExtractionResult {
    private String nomFournisseur;
    private String nomClient;
    private BigDecimal montantTtc;
    private BigDecimal montantHt;
    private BigDecimal tva;
    private LocalDate dateFacture;
    private String numeroFacture;
    private String categorie;
}
