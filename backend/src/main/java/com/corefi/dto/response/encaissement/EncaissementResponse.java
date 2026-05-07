package com.corefi.dto.response.encaissement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    
    // Traçabilité
    private String saisiParNom;
    private LocalDateTime dateSaisie;
    private String valideParNom;
    private LocalDateTime dateValidation;

    // Frais & Compensation
    private BigDecimal fraisTransaction;
    private BigDecimal montantNet;
    private LocalDate datePrevisionnelleCompensation;
    private Long sessionId;
}
