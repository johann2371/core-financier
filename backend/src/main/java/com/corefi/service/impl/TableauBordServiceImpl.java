package com.corefi.service.impl;

import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.entity.CompteFinancier;
import com.corefi.entity.Facture;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.TypeCompte;
import com.corefi.repository.CompteFinancierRepository;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.FactureRepository;
import com.corefi.service.interfaces.ITableauBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableauBordServiceImpl implements ITableauBordService {

    private final CompteFinancierRepository compteFinancierRepository;
    private final DecaissementRepository decaissementRepository;
    private final FactureRepository factureRepository;

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

        // 3. Nombre de décaissements en attente (EN_ATTENTE + EN_ATTENTE_PDG)
        long enAttente = decaissementRepository.findByStatut(StatutDecaissement.EN_ATTENTE).size();
        long enAttentePdg = decaissementRepository.findByStatut(StatutDecaissement.EN_ATTENTE_PDG).size();
        kpis.setDecaissementsEnAttente(enAttente + enAttentePdg);

        // 4. Total des factures impayées (VALIDEE + PARTIELLEMENT_PAYEE)
        List<Facture> factures = factureRepository.findAll();
        BigDecimal totalImpayees = factures.stream()
                .filter(f -> f.getStatut() == StatutFacture.VALIDEE || f.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.setTotalFacturesImpayees(totalImpayees);

        return kpis;
    }
}
