package com.corefi.dto.response.tableaubord;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TableauBordResponse {
    private BigDecimal soldeTotalCaisses;
    private BigDecimal soldeTotalBanques;
    private long decaissementsEnAttente;
    private long decaissementsEnAttenteRF;
    private long decaissementsEnAttentePDG;
    private BigDecimal totalCreancesClients;
    private BigDecimal totalDettesFournisseurs;
    private java.util.Map<String, BigDecimal> repartitionDecaissementsParCategorie;
    private List<ActiviteResponse> activitesRecentes;

    // Derniers mouvements
    private BigDecimal dernierMouvementCaisse;
    private BigDecimal dernierMouvementBanque;
    private BigDecimal derniereCreanceClient;
    private BigDecimal derniereDetteFournisseur;

    // Métriques Stratégiques PDG
    private List<EvolutionMois> evolutionMensuelle;
    private List<TopFournisseur> topFournisseurs;
    private BigDecimal burnRateMensuel;
    private BigDecimal seuilApprobationActuel;

    // Métriques Opérationnelles Caissier
    private long decaissementsAExecuter;
    private BigDecimal montantTotalAExecuter;
    private BigDecimal encaissementsDuJour;
    private BigDecimal decaissementsExecutesDuJour;
    private long operationsDuJour;

    // --- Nouveaux KPIs Admin ---
    private BigDecimal soldeTresorerieTotal;
    private long utilisateursActifs;
    private long utilisateursBloques;
    private long facturesImpayeesCount;
    private long facturesEnRetardCount; // > 30 jours
    private BigDecimal encaissementsMoisActuel;
    private double progressionEncaissements; // % vs mois dernier
    private BigDecimal decaissementsMoisActuel;
    private double progressionDecaissements; // % vs mois dernier
    private long encaissementsDuJourCount;
    private long decaissementsDuJourCount;

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EvolutionMois {
        private String mois;
        private BigDecimal encaissements;
        private BigDecimal decaissements;
    }

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class TopFournisseur {
        private String nom;
        private BigDecimal total;
    }
}
