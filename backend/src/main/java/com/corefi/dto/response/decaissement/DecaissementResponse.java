package com.corefi.dto.response.decaissement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String categorie;
    private String moyenPaiement;
    private int nbJustificatifs;
    private List<JustificatifInfo> justificatifs;
    
    // Traçabilité
    private String saisiParNom;
    private String valideParNom;
    private String approuveParPdgNom;
    private String executeParNom;

    @Data
    public static class JustificatifInfo {
        private Long id;
        private String nomOriginal;
        private String typeFichier;
        private Long tailleFichier;
    }
}
