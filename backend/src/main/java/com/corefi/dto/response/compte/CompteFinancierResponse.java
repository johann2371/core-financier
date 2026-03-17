package com.corefi.dto.response.compte;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CompteFinancierResponse {
    private Long id;
    private String type;     // BANQUE ou CAISSE
    private String numero;
    private String libelle;
    private BigDecimal solde;
    private String deviseCode;  // ex: XAF
    private boolean actif;
}
