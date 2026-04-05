package com.corefi.service.impl;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.entity.Client;
import com.corefi.entity.Fournisseur;
import com.corefi.entity.Tiers;
import com.corefi.enums.TypeTiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.TiersMapper;
import com.corefi.repository.TiersRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.ITiersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TiersServiceImpl implements ITiersService {

    // ── ÉTAPE 2 : implémentation — injecte via les interfaces ────────────────
    private final TiersRepository tiersRepository;
    private final TiersMapper tiersMapper;
    private final IJournalAuditService journalAuditService; // interface ✅

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER UN TIERS (CLIENT ou FOURNISSEUR)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public TiersResponse creer(TiersCreateRequest request) {
        // 1. Valider le type
        TypeTiers type;
        try {
            type = TypeTiers.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Type invalide : " + request.getType()
                    + ". Valeurs acceptées : CLIENT, FOURNISSEUR.");
        }

        // 2. Générer un code unique (ex: CLI-XXXX ou FOU-XXXX)
        String code = genererCode(type);

        // 3. Construire l'entité selon le type
        Tiers tiers;
        if (type == TypeTiers.CLIENT) {
            Client client = new Client();
            client.setTypeClient(request.getTypeClient() != null ? request.getTypeClient() : "ENTREPRISE");
            client.setCni(request.getCni());
            client.setPhotoUrl(request.getPhotoUrl());
            if (request.getCreditLimite() != null)
                client.setCreditLimite(java.math.BigDecimal.valueOf(request.getCreditLimite()));
            if (request.getDelaiPaiement() != null)
                client.setDelaiPaiement(request.getDelaiPaiement());
            tiers = client;
        } else {
            Fournisseur fournisseur = new Fournisseur();
            fournisseur.setNumeroCompte(request.getNumeroCompte());
            fournisseur.setIban(request.getIban());
            tiers = fournisseur;
        }

        // 4. Remplir les champs communs
        tiers.setType(type);
        tiers.setCode(code);
        tiers.setRaisonSociale(request.getRaisonSociale());
        tiers.setTelephone(request.getTelephone());
        tiers.setEmail(request.getEmail());
        tiers.setAdresse(request.getAdresse());
        tiers.setVille(request.getVille());
        tiers.setPays(request.getPays() != null ? request.getPays() : "Cameroun");
        tiers.setNui(request.getNui());
        tiers.setRccm(request.getRccm());
        tiers.setActif(true);

        // 5. Sauvegarder
        Tiers saved = tiersRepository.save(tiers);

        // 6. Audit
        journalAuditService.enregistrer(
                "CREATE", "Tiers", saved.getId(),
                null,
                "Création du tiers " + saved.getCode() + " — " + saved.getRaisonSociale(),
                null
        );

        return tiersMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // TROUVER PAR ID
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public TiersResponse findById(Long id) {
        return tiersMapper.toResponse(trouverOuException(id));
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER TOUS LES TIERS
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public List<TiersResponse> findAll() {
        return tiersRepository.findAll()
                .stream()
                .map(tiersMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER PAR TYPE (CLIENT ou FOURNISSEUR)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public List<TiersResponse> findByType(String type) {
        TypeTiers typeTiers;
        try {
            typeTiers = TypeTiers.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Type invalide : " + type);
        }
        return tiersRepository.findByType(typeTiers)
                .stream()
                .map(tiersMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // METTRE À JOUR
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public TiersResponse mettreAJour(Long id, TiersCreateRequest request) {
        Tiers tiers = trouverOuException(id);

        String anciennesValeurs = "raisonSociale=" + tiers.getRaisonSociale();

        tiers.setRaisonSociale(request.getRaisonSociale());
        tiers.setTelephone(request.getTelephone());
        tiers.setEmail(request.getEmail());
        tiers.setAdresse(request.getAdresse());
        tiers.setVille(request.getVille());
        tiers.setPays(request.getPays());

        // Mise à jour des champs spécifiques
        tiers.setNui(request.getNui());
        tiers.setRccm(request.getRccm());

        if (tiers instanceof Client client) {
            client.setCni(request.getCni());
            client.setPhotoUrl(request.getPhotoUrl());
            if (request.getCreditLimite() != null)
                client.setCreditLimite(java.math.BigDecimal.valueOf(request.getCreditLimite()));
            if (request.getDelaiPaiement() != null)
                client.setDelaiPaiement(request.getDelaiPaiement());
        } else if (tiers instanceof Fournisseur fournisseur) {
            if (request.getNumeroCompte() != null)
                fournisseur.setNumeroCompte(request.getNumeroCompte());
            if (request.getIban() != null)
                fournisseur.setIban(request.getIban());
        }

        Tiers saved = tiersRepository.save(tiers);

        journalAuditService.enregistrer(
                "UPDATE", "Tiers", saved.getId(),
                anciennesValeurs,
                "raisonSociale=" + saved.getRaisonSociale(),
                null
        );

        return tiersMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // DÉSACTIVER (soft delete)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void desactiver(Long id) {
        Tiers tiers = trouverOuException(id);
        tiers.setActif(false);
        tiersRepository.save(tiers);

        journalAuditService.enregistrer(
                "DELETE", "Tiers", id,
                "actif=true",
                "actif=false",
                null
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ────────────────────────────────────────────────────────────────────────
    private Tiers trouverOuException(Long id) {
        return tiersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tiers introuvable avec id : " + id));
    }

    private String genererCode(TypeTiers type) {
        String prefix = type == TypeTiers.CLIENT ? "CLI" : "FOU";
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + "-" + suffix;
    }
}
