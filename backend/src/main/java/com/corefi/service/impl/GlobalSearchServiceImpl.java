package com.corefi.service.impl;

import com.corefi.dto.response.search.SearchResultDTO;
import com.corefi.repository.FactureRepository;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.repository.TiersRepository;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IGlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements IGlobalSearchService {

    private final UtilisateurRepository utilisateurRepository;
    private final TiersRepository tiersRepository;
    private final FactureRepository factureRepository;
    private final JournalAuditRepository journalAuditRepository;

    @Override
    public List<SearchResultDTO> search(String query) {
        List<SearchResultDTO> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) {
            return results;
        }

        String searchTrimmed = query.trim();

        // 1. Recherche Utilisateurs (Optimisée)
        results.addAll(utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrEmailContainingIgnoreCase(searchTrimmed, searchTrimmed, searchTrimmed)
                .stream()
                .limit(5)
                .map(u -> SearchResultDTO.builder()
                        .id(u.getId().toString())
                        .title(u.getPrenom() + " " + u.getNom())
                        .subtitle(u.getEmail() + " | " + u.getRole())
                        .type("USER")
                        .url("/admin")
                        .build())
                .collect(Collectors.toList()));

        // 2. Recherche Tiers (Optimisée)
        results.addAll(tiersRepository.findByRaisonSocialeContainingIgnoreCaseOrCodeContainingIgnoreCase(searchTrimmed, searchTrimmed)
                .stream()
                .limit(5)
                .map(t -> SearchResultDTO.builder()
                        .id(t.getId().toString())
                        .title(t.getRaisonSociale())
                        .subtitle(t.getType() + " | Code: " + t.getCode())
                        .type("TIER")
                        .url("/tiers")
                        .build())
                .collect(Collectors.toList()));

        // 3. Recherche Factures (Optimisée)
        results.addAll(factureRepository.findByNumeroContainingIgnoreCase(searchTrimmed)
                .stream()
                .limit(5)
                .map(f -> SearchResultDTO.builder()
                        .id(f.getId().toString())
                        .title("Facture " + f.getNumero())
                        .subtitle(f.getMontantTtc() + " XAF | " + f.getStatut())
                        .type("INVOICE")
                        .url("/factures")
                        .build())
                .collect(Collectors.toList()));

        // 4. Recherche Audit (Optimisée via existante)
        results.addAll(journalAuditRepository.searchAudit(searchTrimmed, null, org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent().stream()
                .map(a -> SearchResultDTO.builder()
                        .id(a.getId().toString())
                        .title(a.getAction() + " - " + (a.getEntite() != null ? a.getEntite() : "Système"))
                        .subtitle(a.getDateAction().toString().split("T")[0] + " | IP: " + a.getAdresseIp())
                        .type("AUDIT")
                        .url("/audit")
                        .build())
                .collect(Collectors.toList()));

        return results;
    }
}
