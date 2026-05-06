package com.corefi.service.impl;

import com.corefi.entity.*;
import com.corefi.enums.StatutFacture;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IEmailService;
import com.corefi.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service planifié : exécute chaque jour à 7h00 les tâches automatiques :
 * - Relances par mail sur factures échues (J+1, J+7, J+30)
 * - Génération automatique de factures récurrentes
 * - Calcul et application de pénalités de retard
 * - Vérification des budgets (alertes)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationSchedulerService {

    private final FactureRepository factureRepository;
    private final TiersRepository tiersRepository;
    private final BudgetRepository budgetRepository;
    private final DecaissementRepository decaissementRepository;
    private final IEmailService emailService;
    private final INotificationService notificationService;

    // ════════════════════════════════════════════════════════════════════════
    // TÂCHE PLANIFIÉE — Chaque jour à 7h00
    // ════════════════════════════════════════════════════════════════════════
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void executerTachesQuotidiennes() {
        log.info("═══ [SCHEDULER] Démarrage des tâches automatiques quotidiennes ═══");
        envoyerRelancesFacturesEchues();
        appliquerPenalitesRetard();
        verifierBudgets();
        log.info("═══ [SCHEDULER] Tâches terminées ═══");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. RELANCES AUTOMATIQUES PAR EMAIL
    // ════════════════════════════════════════════════════════════════════════
    private void envoyerRelancesFacturesEchues() {
        LocalDate today = LocalDate.now();
        List<Facture> facturesNonSoldees = factureRepository.findAll().stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT
                        || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE)
                .filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(today))
                .filter(f -> "VENTE".equalsIgnoreCase(f.getType()))
                .toList();

        for (Facture f : facturesNonSoldees) {
            long joursRetard = ChronoUnit.DAYS.between(f.getDateEcheance(), today);
            Tiers tiers = f.getTiers();
            String emailClient = tiers != null ? tiers.getEmail() : null;

            // Relances à J+1, J+7, J+30 exactement
            if (joursRetard == 1 || joursRetard == 7 || joursRetard == 30) {
                String sujet = "[SODICA] Rappel - Facture " + f.getNumero() + " échue";
                String corps = construireMessageRelance(f, tiers, joursRetard);

                if (emailClient != null && !emailClient.isBlank()) {
                    try {
                        emailService.sendSimpleMessage(emailClient, sujet, corps);
                        log.info("[RELANCE] Mail envoyé à {} pour facture {} (J+{})", emailClient, f.getNumero(), joursRetard);
                    } catch (Exception e) {
                        log.warn("[RELANCE] Échec envoi mail à {} : {}", emailClient, e.getMessage());
                    }
                }

                // Notification interne aussi
                notificationService.creerEtEnvoyer(
                        "Relance J+" + joursRetard + " — " + f.getNumero(),
                        "La facture " + f.getNumero() + " de " + (tiers != null ? tiers.getRaisonSociale() : "N/A")
                                + " est en retard de " + joursRetard + " jour(s). Montant TTC : "
                                + f.getMontantTtc() + " XAF.",
                        "RESPONSABLE_FINANCIER");
            }
        }
        log.info("[RELANCE] {} facture(s) en retard de paiement détectée(s).", facturesNonSoldees.size());
    }

    private String construireMessageRelance(Facture f, Tiers tiers, long joursRetard) {
        String nomClient = tiers != null ? tiers.getRaisonSociale() : "Client";
        return "Bonjour " + nomClient + ",\n\n"
                + "Nous nous permettons de vous rappeler que votre facture n° " + f.getNumero()
                + " d'un montant de " + f.getMontantTtc() + " XAF "
                + "est arrivée à échéance le " + f.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n"
                + "Soit un retard de " + joursRetard + " jour(s).\n\n"
                + (joursRetard >= 30
                        ? "⚠️ Des pénalités de retard peuvent s'appliquer conformément à nos conditions générales.\n\n"
                        : "")
                + "Nous vous prions de bien vouloir régulariser cette situation dans les meilleurs délais.\n\n"
                + "Cordialement,\n"
                + "SODICA SARL — Service Financier";
    }



    // ════════════════════════════════════════════════════════════════════════
    // 3. PÉNALITÉS DE RETARD
    // ════════════════════════════════════════════════════════════════════════
    private void appliquerPenalitesRetard() {
        LocalDate today = LocalDate.now();
        // Taux de pénalité : 1.5% du montant TTC par mois de retard (proratisé au jour)
        BigDecimal tauxJournalier = new BigDecimal("0.0005"); // ~1.5% / 30 jours

        List<Facture> facturesEnRetard = factureRepository.findAll().stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT
                        || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE)
                .filter(f -> "VENTE".equalsIgnoreCase(f.getType()))
                .filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(today))
                .toList();

        for (Facture f : facturesEnRetard) {
            long joursRetard = ChronoUnit.DAYS.between(f.getDateEcheance(), today);
            if (joursRetard <= 0) continue;

            // Pénalité = montantTTC × taux journalier × nombre de jours de retard
            BigDecimal penalite = f.getMontantTtc()
                    .multiply(tauxJournalier)
                    .multiply(BigDecimal.valueOf(joursRetard))
                    .setScale(2, RoundingMode.HALF_UP);

            // On ne fait qu'ajouter la pénalité au solde du tiers (pas au montant facture)
            Tiers tiers = f.getTiers();
            if (tiers != null && tiers.getSolde() != null) {
                // On ne recalcule pas chaque jour, on log seulement tous les 30 jours
                if (joursRetard % 30 == 0) {
                    tiers.setSolde(tiers.getSolde().add(penalite));
                    tiersRepository.save(tiers);
                    log.info("[PÉNALITÉ] {} XAF ajoutés au solde de {} pour facture {} ({}j de retard)",
                            penalite, tiers.getRaisonSociale(), f.getNumero(), joursRetard);

                    notificationService.creerEtEnvoyer(
                            "Pénalité de retard appliquée",
                            "Une pénalité de " + penalite + " XAF a été appliquée à "
                                    + tiers.getRaisonSociale() + " pour la facture "
                                    + f.getNumero() + " (" + joursRetard + " jours de retard).",
                            "RESPONSABLE_FINANCIER");
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. VÉRIFICATION DES BUDGETS
    // ════════════════════════════════════════════════════════════════════════
    private void verifierBudgets() {
        int annee = LocalDate.now().getYear();
        int mois = LocalDate.now().getMonthValue();

        List<Budget> budgets = budgetRepository.findByAnneeAndMois(annee, mois);
        budgets.addAll(budgetRepository.findByAnneeAndMois(annee, 0)); // budgets annuels

        for (Budget b : budgets) {
            if (b.getMontantPlafond().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal pourcent = b.getMontantConsomme()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(b.getMontantPlafond(), 2, RoundingMode.HALF_UP);

            if (pourcent.compareTo(BigDecimal.valueOf(b.getSeuilAlertePourcent())) >= 0 && !b.isAlerteEnvoyee()) {
                b.setAlerteEnvoyee(true);
                budgetRepository.save(b);

                String msg = "Le budget " + b.getCategorie().name() + " (" + (b.getMois() == 0 ? "annuel" : "mois " + b.getMois()) + ") "
                        + "a atteint " + pourcent + "% (" + b.getMontantConsomme() + " / " + b.getMontantPlafond() + " XAF).";

                notificationService.creerEtEnvoyer("⚠️ Alerte Budget", msg, "RESPONSABLE_FINANCIER");
                notificationService.creerEtEnvoyer("⚠️ Alerte Budget", msg, "PDG");
                log.warn("[BUDGET] {}", msg);
            }
        }
    }

    private String genererNumeroFacture() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = factureRepository.count() + 1;
        return String.format("FAC-%s-%04d", datePart, count);
    }
}
