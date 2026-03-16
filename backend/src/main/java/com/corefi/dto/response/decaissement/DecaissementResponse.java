package com.corefi.dto.response.decaissement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DecaissementResponse {
    private Long id;
    private String numero;
    private LocalDate dateDecaissement;
    private String fournisseurNom;
    private BigDecimal montant;
    private String statut;
    private boolean seuilPdgRequis;
}
