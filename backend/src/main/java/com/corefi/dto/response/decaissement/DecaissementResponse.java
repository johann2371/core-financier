package com.corefi.dto.response.decaissement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DecaissementResponse {
    private Long id;
    private String numero;
    private LocalDate dateDecaissement;
    private LocalDateTime dateCreation;
    private Long fournisseurId;
    private String fournisseurNom;
    private String beneficiaire;
    private String motif;
    private BigDecimal montant;
    private String statut;
    private boolean seuilPdgRequis;
}
