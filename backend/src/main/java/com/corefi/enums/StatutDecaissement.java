package com.corefi.enums;

public enum StatutDecaissement {
    BROUILLON, // Comptable : en cours de saisie
    EN_ATTENTE, // Soumis, en attente Responsable Financier
    VALIDEE_RF, // Validé par RF (montant < seuil PDG)
    EN_ATTENTE_PDG, // Transmis au PDG (montant >= seuil)
    VALIDEE_PDG, // Approuvé par PDG, en attente Caissier
    EXECUTEE, // Caissier a exécuté le paiement
    REJETEE_RF, // Rejeté par le Responsable Financier
    REJETEE_PDG, // Rejeté par le PDG
    ANNULEE
}
