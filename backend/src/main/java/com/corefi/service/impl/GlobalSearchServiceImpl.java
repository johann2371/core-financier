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

        String lowerQuery = query.toLowerCase();

        // 1. Recherche Utilisateurs
        results.addAll(utilisateurRepository.findAll().stream()
                .filter(u -> u.getNom().toLowerCase().contains(lowerQuery) || 
                             u.getPrenom().toLowerCase().contains(lowerQuery) || 
                             u.getEmail().toLowerCase().contains(lowerQuery))
                .limit(5)
                .map(u -> SearchResultDTO.builder()
                        .id(u.getId().toString())
                        .title(u.getPrenom() + " " + u.getNom())
                        .subtitle(u.getEmail() + " | " + u.getRole())
                        .type("USER")
                        .url("/admin") // Redirige vers manage users
                        .build())
                .collect(Collectors.toList()));

        // 2. Recherche Tiers
        results.addAll(tiersRepository.findAll().stream()
                .filter(t -> (t.getRaisonSociale() != null && t.getRaisonSociale().toLowerCase().contains(lowerQuery)) || 
                             (t.getCode() != null && t.getCode().toLowerCase().contains(lowerQuery)))
                .limit(5)
                .map(t -> SearchResultDTO.builder()
                        .id(t.getId().toString())
                        .title(t.getRaisonSociale())
                        .subtitle(t.getType() + " | Code: " + t.getCode())
                        .type("TIER")
                        .url("/tiers")
                        .build())
                .collect(Collectors.toList()));

        // 3. Recherche Factures
        results.addAll(factureRepository.findAll().stream()
                .filter(f -> f.getNumero().toLowerCase().contains(lowerQuery))
                .limit(5)
                .map(f -> SearchResultDTO.builder()
                        .id(f.getId().toString())
                        .title("Facture " + f.getNumero())
                        .subtitle(f.getMontantTtc() + " XAF | " + f.getStatut())
                        .type("INVOICE")
                        .url("/factures")
                        .build())
                .collect(Collectors.toList()));

        // 4. Recherche Audit
        results.addAll(journalAuditRepository.findAll().stream()
                .filter(a -> a.getAction().toLowerCase().contains(lowerQuery) || 
                             (a.getEntite() != null && a.getEntite().toLowerCase().contains(lowerQuery)))
                .limit(5)
                .map(a -> SearchResultDTO.builder()
                        .id(a.getId().toString())
                        .title(a.getAction() + " - " + a.getEntite())
                        .subtitle(a.getDateAction().toString() + " | IP: " + a.getAdresseIp())
                        .type("AUDIT")
                        .url("/audit")
                        .build())
                .collect(Collectors.toList()));

        return results;
    }
}
