package com.corefi.dto.response.encaissement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EncaissementResponse {
    private Long id;
    private String numero;
    private LocalDate dateEncaissement;
    private Long clientId;
    private String nomClient;
    private BigDecimal montant;
    private String reference;
    private String moyenPaiement;
    private String statut;
}
