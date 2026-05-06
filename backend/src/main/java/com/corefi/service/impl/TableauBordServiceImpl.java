package com.corefi.service.impl;

import com.corefi.dto.response.tableaubord.ActiviteResponse;
import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.entity.CompteFinancier;
import com.corefi.entity.Facture;
import com.corefi.entity.Decaissement;
import com.corefi.entity.Encaissement;
import com.corefi.entity.JournalAudit;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.TypeCompte;
import com.corefi.repository.CompteFinancierRepository;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.FactureRepository;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.service.interfaces.ITableauBordService;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import com.corefi.entity.Parametrage;
import com.corefi.entity.Utilisateur;
import com.corefi.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.corefi.repository.EncaissementRepository;
import com.corefi.repository.ParametrageRepository;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import com.corefi.repository.UtilisateurRepository;

@Service
@Slf4j
public class TableauBordServiceImpl implements ITableauBordService {

    private final CompteFinancierRepository compteFinancierRepository;
    private final DecaissementRepository decaissementRepository;
    private final EncaissementRepository encaissementRepository;
    private final FactureRepository factureRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final ParametrageRepository parametrageRepository;
    private final UtilisateurRepository utilisateurRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private IJournalAuditService journalAuditService;

    @org.springframework.beans.factory.annotation.Autowired
    private INotificationService notificationService;

    public TableauBordServiceImpl(
            CompteFinancierRepository compteFinancierRepository,
            DecaissementRepository decaissementRepository,
            EncaissementRepository encaissementRepository,
            FactureRepository factureRepository,
            JournalAuditRepository journalAuditRepository,
            ParametrageRepository parametrageRepository,
            UtilisateurRepository utilisateurRepository) {
        this.compteFinancierRepository = compteFinancierRepository;
        this.decaissementRepository = decaissementRepository;
        this.encaissementRepository = encaissementRepository;
        this.factureRepository = factureRepository;
        this.journalAuditRepository = journalAuditRepository;
        this.parametrageRepository = parametrageRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TableauBordResponse getKpis(int forecastDays) {
        TableauBordResponse kpis = new TableauBordResponse();

        // 1. Solde total
        List<CompteFinancier> comptes = compteFinancierRepository.findAll();
        BigDecimal soldeCaisses = comptes.stream()
                .filter(c -> c.getType() == TypeCompte.CAISSE && c.isActif())
                .map(CompteFinancier::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldeBanques = comptes.stream()
                .filter(c -> c.getType() == TypeCompte.BANQUE && c.isActif())
                .map(CompteFinancier::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        kpis.setSoldeTotalCaisses(soldeCaisses);
        kpis.setSoldeTotalBanques(soldeBanques);
        kpis.setSoldeTresorerieTotal(soldeCaisses.add(soldeBanques));

        // 2. Nombre de décaissements en attente (Pipeline)
        List<Decaissement> allDecaissements = decaissementRepository.findAll();
        long enAttenteRF = allDecaissements.stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EN_ATTENTE)
                .count();
        long enAttentePDG = allDecaissements.stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EN_ATTENTE_PDG)
                .count();
        
        kpis.setDecaissementsEnAttente(enAttenteRF + enAttentePDG);
        kpis.setDecaissementsEnAttenteRF(enAttenteRF);
        kpis.setDecaissementsEnAttentePDG(enAttentePDG);

        // 3. Utilisateurs
        List<Utilisateur> users = utilisateurRepository.findAll();
        kpis.setUtilisateursActifs(users.stream().filter(Utilisateur::isActif).count());
        kpis.setUtilisateursBloques(users.stream().filter(u -> !u.isActif()).count());

        // 4. Factures Impayées & Retard (> 30j)
        List<Facture> factures = factureRepository.findAll();
        java.time.LocalDate limitDate = java.time.LocalDate.now().minusDays(30);
        
        long countImpayees = factures.stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE)
                .count();
        long countRetard = factures.stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE)
                .filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(limitDate))
                .count();
        
        kpis.setFacturesImpayeesCount(countImpayees);
        kpis.setFacturesEnRetardCount(countRetard);

        // 5. Créances & Dettes (Montants)
        BigDecimal totalCreances = factures.stream()
                .filter(f -> "VENTE".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalCreancesClients(totalCreances);

        BigDecimal totalDettes = factures.stream()
                .filter(f -> "ACHAT".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalDettesFournisseurs(totalDettes);

        // 6. Statistiques Mensuelles & Progression
        calculerProgressions(kpis);

        // 6b. Calcul des derniers mouvements (Tendance)
        calculerDerniersMouvements(kpis);

        // 7. Activités récentes (Audit)
        kpis.setActivitesRecentes(
            journalAuditRepository.findAllByOrderByDateActionDesc(PageRequest.of(0, 10))
                .getContent().stream()
                .map(this::mapToActiviteResponse)
                .collect(Collectors.toList())
        );

        // 8. Métriques Stratégiques & Opérationnelles
        kpis.setEvolutionMensuelle(calculerEvolutionMensuelle());
        kpis.setTopFournisseurs(calculerTopFournisseurs());
        kpis.setBurnRateMensuel(calculerBurnRate());
        kpis.setSeuilApprobationActuel(recupererSeuil("SEUIL_APPROBATION_PDG", new BigDecimal("500000")));
        calculerMetriquesCaissier(kpis, allDecaissements);

        // 9. KPIs Stratégiques (DSO, DPO, Prévision)
        calculerDsoDpo(kpis, factures);
        calculerPrevisionTresorerie(kpis, factures, soldeCaisses.add(soldeBanques), forecastDays);
        calculerRepartitionDepenses(kpis, allDecaissements);

        return kpis;
    }

    private void calculerProgressions(TableauBordResponse kpis) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        // Encaissements
        BigDecimal encMoisActuel = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(currentMonth))
                .map(Encaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal encMoisDernier = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(lastMonth))
                .map(Encaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        kpis.setEncaissementsMoisActuel(encMoisActuel);
        kpis.setProgressionEncaissements(calculerPourcentage(encMoisActuel, encMoisDernier));

        // Décaissements
        BigDecimal decMoisActuel = decaissementRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(currentMonth))
                .map(Decaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal decMoisDernier = decaissementRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(lastMonth))
                .map(Decaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        kpis.setDecaissementsMoisActuel(decMoisActuel);
        kpis.setProgressionDecaissements(calculerPourcentage(decMoisActuel, decMoisDernier));
    }

    private double calculerPourcentage(BigDecimal actuel, BigDecimal dernier) {
        if (dernier == null || dernier.compareTo(BigDecimal.ZERO) == 0) return 100.0;
        return (actuel.subtract(dernier)).divide(dernier, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue();
    }

    private void calculerMetriquesCaissier(TableauBordResponse kpis, List<Decaissement> allDecaissements) {
        java.time.LocalDate today = java.time.LocalDate.now();

        // Dossiers prêts à exécuter (validés par RF ou PDG)
        List<Decaissement> aExecuter = allDecaissements.stream()
                .filter(d -> d.getStatut() == StatutDecaissement.VALIDEE_RF || d.getStatut() == StatutDecaissement.VALIDEE_PDG)
                .collect(Collectors.toList());
        kpis.setDecaissementsAExecuter(aExecuter.size());
        kpis.setMontantTotalAExecuter(aExecuter.stream().map(Decaissement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Encaissements du jour
        BigDecimal encDuJour = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && e.getDateEncaissement().equals(today))
                .map(e -> e.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setEncaissementsDuJour(encDuJour);

        // Décaissements exécutés du jour
        List<Decaissement> execDuJour = allDecaissements.stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && d.getDateExecution().toLocalDate().equals(today))
                .collect(Collectors.toList());
        kpis.setDecaissementsExecutesDuJour(execDuJour.stream().map(Decaissement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Nombre total d'opérations du jour (encaissements + décaissements exécutés)
        long nbEncDuJour = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && e.getDateEncaissement().equals(today))
                .count();
        kpis.setEncaissementsDuJourCount(nbEncDuJour);
        kpis.setDecaissementsDuJourCount((long) execDuJour.size());
        kpis.setOperationsDuJour(nbEncDuJour + execDuJour.size());
    }

    private void calculerDerniersMouvements(TableauBordResponse kpis) {
        // Dernier mouvement Caisse
        kpis.setDernierMouvementCaisse(getDernierMouvement(TypeCompte.CAISSE));
        
        // Dernier mouvement Banque
        kpis.setDernierMouvementBanque(getDernierMouvement(TypeCompte.BANQUE));

        // Dernière créance client (Dernière facture VENTE ou dernier encaissement)
        kpis.setDerniereCreanceClient(getDernierMouvementClient());

        // Dernière dette fournisseur (Dernière facture ACHAT ou dernier décaissement)
        kpis.setDerniereDetteFournisseur(getDernierMouvementFournisseur());
    }

    private List<TableauBordResponse.EvolutionMois> calculerEvolutionMensuelle() {
        List<TableauBordResponse.EvolutionMois> result = new ArrayList<>();
        YearMonth current = YearMonth.now();
        
        for (int i = 2; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            
            BigDecimal enc = encaissementRepository.findAll().stream()
                    .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(ym))
                    .map(e -> e.getMontant())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dec = decaissementRepository.findAll().stream()
                    .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(ym))
                    .map(d -> d.getMontant())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.add(new TableauBordResponse.EvolutionMois(label, enc, dec));
        }
        return result;
    }

    private List<TableauBordResponse.TopFournisseur> calculerTopFournisseurs() {
        Map<String, BigDecimal> stats = new HashMap<>();
        decaissementRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getFournisseur() != null)
                .forEach(d -> {
                    String nom = d.getFournisseur().getRaisonSociale();
                    stats.put(nom, stats.getOrDefault(nom, BigDecimal.ZERO).add(d.getMontant()));
                });
        
        return stats.entrySet().stream()
                .map(e -> new TableauBordResponse.TopFournisseur(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private BigDecimal calculerBurnRate() {
        YearMonth current = YearMonth.now();
        BigDecimal total = BigDecimal.ZERO;
        int monthsWithData = 0;
        for (int i = 1; i <= 3; i++) {
            YearMonth ym = current.minusMonths(i);
            BigDecimal dec = decaissementRepository.findAll().stream()
                    .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(ym))
                    .map(d -> d.getMontant())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(dec);
            monthsWithData++;
        }
        return monthsWithData > 0 ? total.divide(new BigDecimal(monthsWithData), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal getDernierMouvement(TypeCompte type) {
        Optional<com.corefi.entity.Encaissement> lastEnc = encaissementRepository.findAll().stream()
                .filter(e -> e.getCompteFinancier() != null && e.getCompteFinancier().getType() == type)
                .max(Comparator.comparing(com.corefi.entity.Encaissement::getDateEncaissement));
        
        Optional<Decaissement> lastDec = decaissementRepository.findAll().stream()
                .filter(d -> d.getCompteFinancier() != null && d.getCompteFinancier().getType() == type)
                .max(Comparator.comparing(Decaissement::getDateSaisie));

        if (lastEnc.isPresent() && lastDec.isPresent()) {
            // Comparaison simplifiée sur LocalDate car Encaissement n'a pas LocalDateTime pour la saisie technique
            if (lastEnc.get().getDateEncaissement().isAfter(lastDec.get().getDateSaisie().toLocalDate())) {
                return lastEnc.get().getMontant();
            } else {
                return lastDec.get().getMontant().negate();
            }
        } else if (lastEnc.isPresent()) {
            return lastEnc.get().getMontant();
        } else if (lastDec.isPresent()) {
            return lastDec.get().getMontant().negate();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getDernierMouvementClient() {
        Optional<Facture> lastFact = factureRepository.findAll().stream()
                .filter(f -> "VENTE".equals(f.getType()))
                .max(Comparator.comparing(Facture::getDateFacture));
        
        Optional<com.corefi.entity.Encaissement> lastEnc = encaissementRepository.findAll().stream()
                .max(Comparator.comparing(com.corefi.entity.Encaissement::getDateEncaissement));

        if (lastFact.isPresent() && lastEnc.isPresent()) {
            if (lastFact.get().getDateFacture().isAfter(lastEnc.get().getDateEncaissement())) {
                return lastFact.get().getMontantTtc();
            } else {
                return lastEnc.get().getMontant().negate();
            }
        } else if (lastFact.isPresent()) {
            return lastFact.get().getMontantTtc();
        } else if (lastEnc.isPresent()) {
            return lastEnc.get().getMontant().negate();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getDernierMouvementFournisseur() {
        Optional<Facture> lastFact = factureRepository.findAll().stream()
                .filter(f -> "ACHAT".equals(f.getType()))
                .max(Comparator.comparing(Facture::getDateFacture));
        
        Optional<Decaissement> lastDec = decaissementRepository.findAll().stream()
                .max(Comparator.comparing(Decaissement::getDateSaisie));

        if (lastFact.isPresent() && lastDec.isPresent()) {
            if (lastFact.get().getDateFacture().isAfter(lastDec.get().getDateSaisie().toLocalDate())) {
                return lastFact.get().getMontantTtc();
            } else {
                return lastDec.get().getMontant().negate();
            }
        } else if (lastFact.isPresent()) {
            return lastFact.get().getMontantTtc();
        } else if (lastDec.isPresent()) {
            return lastDec.get().getMontant().negate();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal recupererSeuil(String cle, BigDecimal parDefaut) {
        return parametrageRepository.findByCle(cle)
                .map(p -> {
                    try {
                        return new BigDecimal(p.getValeur());
                    } catch (Exception e) {
                        return parDefaut;
                    }
                })
                .orElse(parDefaut);
    }

    @Override
    @Transactional
    public void updateSeuil(BigDecimal nouveauSeuil) {
        String cle = "SEUIL_APPROBATION_PDG";
        Parametrage p = parametrageRepository.findByCle(cle)
                .orElse(new Parametrage(null, cle, "500000", "Seuil d'approbation PDG", null));
        
        p.setValeur(nouveauSeuil.toString());
        p.setModifiePar(getUtilisateurConnecte());
        parametrageRepository.save(p);
        
        journalAuditService.enregistrer("UPDATE", "Parametrage", p.getId(), "seuil", "Nouveau seuil PDG : " + nouveauSeuil + " XAF", null);
        
        notificationService.creerEtEnvoyer(
            "Seuil d'approbation modifié",
            "Le seuil d'approbation des décaissements a été mis à jour à " + nouveauSeuil + " XAF.",
            "RESPONSABLE_FINANCIER"
        );
    }

    private Utilisateur getUtilisateurConnecte() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));
    }

    private ActiviteResponse mapToActiviteResponse(JournalAudit audit) {
        String type = "SYSTEM";
        if ("Facture".equals(audit.getEntite())) type = "FACTURE";
        else if ("Encaissement".equals(audit.getEntite())) type = "ENCAISSEMENT";
        else if ("Decaissement".equals(audit.getEntite())) type = "DECAISSEMENT";
        else if ("Tiers".equals(audit.getEntite())) type = "TIERS";
        else if ("Utilisateur".equals(audit.getEntite())) type = "AUTH";

        String fullName = audit.getUtilisateur() != null ? audit.getUtilisateur().getPrenom() + " " + audit.getUtilisateur().getNom() : "Système";
        String rawMessage = audit.getNouvellesValeurs() != null ? audit.getNouvellesValeurs() : "";
        String finalMessage = rawMessage;

        // Transformation narrative selon l'entité et l'action
        if ("Encaissement".equals(audit.getEntite()) && "CREATE".equals(audit.getAction())) {
            finalMessage = fullName + " a enregistré un encaissement — " + extractMontant(rawMessage);
        } else if ("Decaissement".equals(audit.getEntite())) {
            if ("CREATE".equals(audit.getAction())) {
                finalMessage = fullName + " a soumis une demande de décaissement — " + extractMontant(rawMessage);
            } else if ("UPDATE".equals(audit.getAction())) {
                if (rawMessage.contains("EXECUTEE")) {
                    finalMessage = fullName + " a exécuté le paiement " + extractNumero(rawMessage) + " — " + extractMontant(rawMessage);
                } else if (rawMessage.contains("VALIDEE")) {
                    finalMessage = fullName + " a validé le décaissement " + extractNumero(rawMessage);
                }
            }
        } else if ("Facture".equals(audit.getEntite()) && "CREATE".equals(audit.getAction())) {
            finalMessage = fullName + " a créé la facture " + extractNumero(rawMessage) + " — " + extractMontant(rawMessage);
        } else if ("Utilisateur".equals(audit.getEntite()) && rawMessage.contains("échouée")) {
            finalMessage = rawMessage; // On garde tel quel pour l'auth (ex: "3 tentatives de connexion échouées — ...")
        }

        return ActiviteResponse.builder()
            .type(type)
            .action(audit.getAction())
            .message(finalMessage)
            .date(audit.getDateAction())
            .utilisateur(fullName)
            .build();
    }

    private String extractMontant(String message) {
        if (message == null) return "0 FCFA";
        // Cherche un montant suivi de XAF ou FCFA
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+[\\s\\d.]*)").matcher(message);
        if (m.find()) return m.group(1).trim() + " FCFA";
        return "0 FCFA";
    }

    private String extractNumero(String message) {
        if (message == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(DEC-\\S+|FAC-\\S+|ENC-\\S+)").matcher(message);
        if (m.find()) return "#" + m.group(1);
        return "";
    }

    // ==================== KPIS STRATEGIQUES ====================

    /**
     * Calcule le DSO (Days Sales Outstanding) et DPO (Days Payables Outstanding).
     * DSO = délai moyen de paiement des clients (factures de vente payées).
     * DPO = délai moyen de règlement aux fournisseurs (factures d'achat payées).
     */
    private void calculerDsoDpo(TableauBordResponse kpis, List<Facture> factures) {
        // DSO: Approximation basée sur les factures VENTE payées
        // Délai moyen = différence entre dateEchéance et dateFacture pour les factures payées
        double totalDsoJours = 0;
        int countDso = 0;

        for (Facture f : factures) {
            if ("VENTE".equals(f.getType()) && f.getStatut() == StatutFacture.SOLDEE
                    && f.getDateFacture() != null && f.getDateEcheance() != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(f.getDateFacture(), f.getDateEcheance());
                if (jours >= 0) {
                    totalDsoJours += jours;
                    countDso++;
                }
            }
        }
        kpis.setDso(countDso > 0 ? Math.round(totalDsoJours / countDso * 10.0) / 10.0 : 0);

        // DPO: Factures ACHAT payées -> différence entre date facture et date décaissement
        List<Decaissement> allDec = decaissementRepository.findAll();
        double totalDpoJours = 0;
        int countDpo = 0;

        for (Decaissement d : allDec) {
            if (d.getStatut() == StatutDecaissement.EXECUTEE && d.getDateExecution() != null && d.getDateSaisie() != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(d.getDateSaisie().toLocalDate(), d.getDateExecution().toLocalDate());
                if (jours >= 0) {
                    totalDpoJours += jours;
                    countDpo++;
                }
            }
        }
        kpis.setDpo(countDpo > 0 ? Math.round(totalDpoJours / countDpo * 10.0) / 10.0 : 0);
    }

    /**
     * Calcule la prévision de trésorerie sur X jours.
     * Logique : Solde actuel + Factures Vente échéance < Xj - Factures Achat échéance < Xj
     */
    private void calculerPrevisionTresorerie(TableauBordResponse kpis, List<Facture> factures, BigDecimal soldeActuel, int forecastDays) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate limit = today.plusDays(forecastDays);

        // Factures impayées avec échéance dans les X prochains jours
        List<Facture> ventesAttendues = factures.stream()
                .filter(f -> "VENTE".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .filter(f -> f.getDateEcheance() != null && !f.getDateEcheance().isBefore(today) && !f.getDateEcheance().isAfter(limit))
                .collect(Collectors.toList());

        List<Facture> achatsAttendus = factures.stream()
                .filter(f -> "ACHAT".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .filter(f -> f.getDateEcheance() != null && !f.getDateEcheance().isBefore(today) && !f.getDateEcheance().isAfter(limit))
                .collect(Collectors.toList());

        // Générer les points de projection dynamiquement
        List<TableauBordResponse.PointPrevision> points = new ArrayList<>();
        List<Integer> checkpoints = new ArrayList<>();
        checkpoints.add(0);
        int step = forecastDays <= 30 ? 7 : (forecastDays <= 60 ? 15 : 30);
        for (int i = step; i < forecastDays; i += step) {
            checkpoints.add(i);
        }
        checkpoints.add(forecastDays);

        for (int jour : checkpoints) {
            java.time.LocalDate datePoint = today.plusDays(jour);

            BigDecimal entreesCumul = ventesAttendues.stream()
                    .filter(f -> !f.getDateEcheance().isAfter(datePoint))
                    .map(Facture::getMontantTtc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sortiesCumul = achatsAttendus.stream()
                    .filter(f -> !f.getDateEcheance().isAfter(datePoint))
                    .map(Facture::getMontantTtc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal soldePrevu = soldeActuel.add(entreesCumul).subtract(sortiesCumul);
            String label = jour == 0 ? "Aujourd'hui" : "J+" + jour;
            points.add(new TableauBordResponse.PointPrevision(label, soldePrevu));
        }

        kpis.setPointsPrevisionnels(points);
        kpis.setSoldePrevisionnel30j(points.get(points.size() - 1).getSolde());
    }

    /**
     * Répartition des dépenses par catégorie (basé sur le champ "categorie" du décaissement).
     */
    private void calculerRepartitionDepenses(TableauBordResponse kpis, List<Decaissement> allDecaissements) {
        YearMonth currentMonth = YearMonth.now();
        Map<String, BigDecimal> repartition = new LinkedHashMap<>();

        allDecaissements.stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE)
                .filter(d -> d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(currentMonth))
                .forEach(d -> {
                    String cat = d.getCategorie() != null ? d.getCategorie().name() : "Autre";
                    repartition.put(cat, repartition.getOrDefault(cat, BigDecimal.ZERO).add(d.getMontant()));
                });

        kpis.setRepartitionDepensesParCategorie(repartition);
    }
}
