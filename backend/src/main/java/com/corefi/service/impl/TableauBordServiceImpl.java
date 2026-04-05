package com.corefi.service.impl;

import com.corefi.dto.response.tableaubord.ActiviteResponse;
import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.entity.CompteFinancier;
import com.corefi.entity.Facture;
import com.corefi.entity.Decaissement;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class TableauBordServiceImpl implements ITableauBordService {

    private final CompteFinancierRepository compteFinancierRepository;
    private final DecaissementRepository decaissementRepository;
    private final EncaissementRepository encaissementRepository;
    private final FactureRepository factureRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final ParametrageRepository parametrageRepository;
    private final com.corefi.repository.UtilisateurRepository utilisateurRepository;
    private final IJournalAuditService journalAuditService;
    private final INotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public TableauBordResponse getKpis() {
        TableauBordResponse kpis = new TableauBordResponse();

        // 1. Solde total des caisses
        List<CompteFinancier> comptes = compteFinancierRepository.findAll();
        BigDecimal soldeCaisses = comptes.stream()
                .filter(c -> c.getType() == TypeCompte.CAISSE && c.isActif())
                .map(CompteFinancier::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setSoldeTotalCaisses(soldeCaisses);

        // 2. Solde total des banques
        BigDecimal soldeBanques = comptes.stream()
                .filter(c -> c.getType() == TypeCompte.BANQUE && c.isActif())
                .map(CompteFinancier::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setSoldeTotalBanques(soldeBanques);

        // 3. Nombre de décaissements en attente (Pipeline)
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

        // 3b. Répartition par catégorie (sur les décaissements validés ou exécutés)
        java.util.Map<String, BigDecimal> repartition = allDecaissements.stream()
                .filter(d -> d.getStatut() != StatutDecaissement.BROUILLON && d.getStatut() != StatutDecaissement.REJETEE_RF && d.getStatut() != StatutDecaissement.REJETEE_PDG && d.getStatut() != StatutDecaissement.ANNULEE)
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getCategorie() != null ? d.getCategorie().name() : "AUTRE",
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Decaissement::getMontant, BigDecimal::add)
                ));
        kpis.setRepartitionDecaissementsParCategorie(repartition);

        // 4. Créances Clients (Factures de VENTE impayées)
        // 4. Créances Clients (Factures de VENTE impayées)
        List<Facture> factures = factureRepository.findAll();
        BigDecimal totalCreances = factures.stream()
                .filter(f -> "VENTE".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalCreancesClients(totalCreances);

        // 5. Dettes Fournisseurs (Factures d'ACHAT impayées)
        BigDecimal totalDettes = factures.stream()
                .filter(f -> "ACHAT".equals(f.getType()))
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalDettesFournisseurs(totalDettes);

        // 5. Activités récentes (Audit)
        kpis.setActivitesRecentes(
            journalAuditRepository.findAllByOrderByDateActionDesc(PageRequest.of(0, 10))
                .getContent().stream()
                .map(this::mapToActiviteResponse)
                .collect(Collectors.toList())
        );

        // 6. Calcul des derniers mouvements (Tendance)
        calculerDerniersMouvements(kpis);

        // 7. Métriques Stratégiques PDG
        kpis.setEvolutionMensuelle(calculerEvolutionMensuelle());
        kpis.setTopFournisseurs(calculerTopFournisseurs());
        kpis.setBurnRateMensuel(calculerBurnRate());
        kpis.setSeuilApprobationActuel(recupererSeuil("SEUIL_APPROBATION_PDG", new BigDecimal("500000")));

        // 8. Métriques Opérationnelles Caissier
        calculerMetriquesCaissier(kpis, allDecaissements);

        log.info("Dashboard KPIs: Caisses={}, Banques={}, Creances={}, Dettes={}, Activites={}", 
                soldeCaisses, soldeBanques, totalCreances, totalDettes, kpis.getActivitesRecentes().size());
        
        return kpis;
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
        
        for (int i = 5; i >= 0; i--) {
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

        return ActiviteResponse.builder()
            .type(type)
            .action(audit.getAction())
            .message(audit.getNouvellesValeurs()) // On utilise le champ nouvellesValeurs qui contient souvent le message
            .date(audit.getDateAction())
            .utilisateur(audit.getUtilisateur() != null ? audit.getUtilisateur().getPrenom() + " " + audit.getUtilisateur().getNom() : "Système")
            .build();
    }
}
