package com.corefi.service.impl;

import com.corefi.dto.response.tableaubord.ActiviteResponse;
import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.entity.CompteFinancier;
import com.corefi.entity.Facture;
import com.corefi.entity.JournalAudit;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.TypeCompte;
import com.corefi.repository.CompteFinancierRepository;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.FactureRepository;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.service.interfaces.ITableauBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableauBordServiceImpl implements ITableauBordService {

    private final CompteFinancierRepository compteFinancierRepository;
    private final DecaissementRepository decaissementRepository;
    private final FactureRepository factureRepository;
    private final JournalAuditRepository journalAuditRepository;

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

        // 3. Nombre de décaissements en attente
        long enAttente = decaissementRepository.findByStatut(StatutDecaissement.EN_ATTENTE).size();
        long enAttentePdg = decaissementRepository.findByStatut(StatutDecaissement.EN_ATTENTE_PDG).size();
        kpis.setDecaissementsEnAttente(enAttente + enAttentePdg);

        // 4. Total des factures impayées
        List<Facture> factures = factureRepository.findAll();
        BigDecimal totalImpayees = factures.stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE || f.getStatut() == StatutFacture.VALIDEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalFacturesImpayees(totalImpayees);

        // 5. Activités récentes (Audit)
        kpis.setActivitesRecentes(
            journalAuditRepository.findAllByOrderByDateActionDesc(PageRequest.of(0, 10))
                .getContent().stream()
                .map(this::mapToActiviteResponse)
                .collect(Collectors.toList())
        );

        return kpis;
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
