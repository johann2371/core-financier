package com.corefi.service.impl;

import com.corefi.dto.request.encaissement.AffectationRequest;
import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.entity.*;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutEncaissement;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.TypeTiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.EncaissementMapper;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IEncaissementService;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaissementServiceImpl implements IEncaissementService {

    private final EncaissementRepository encaissementRepository;
    private final TiersRepository tiersRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final DeviseRepository deviseRepository;
    private final FactureRepository factureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EncaissementMapper encaissementMapper;
    private final IJournalAuditService journalAuditService;

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER UN ENCAISSEMENT (simulation de paiement reçu)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public EncaissementResponse creer(EncaissementCreateRequest request) {
        // 1. Vérifier le client
        Tiers client = tiersRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec ID : " + request.getClientId()));

        if (client.getType() != TypeTiers.CLIENT) {
            throw new WorkflowException("Un encaissement doit être associé à un CLIENT, pas un FOURNISSEUR.");
        }

        // 2. Vérifier le compte financier
        CompteFinancier compte = compteFinancierRepository.findById(request.getCompteFinancierId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte financier introuvable avec ID : " + request.getCompteFinancierId()));

        // 3. Vérifier la devise (XAF par défaut)
        Devise devise = null;
        if (request.getDeviseId() != null) {
            devise = deviseRepository.findById(request.getDeviseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise introuvable avec ID : " + request.getDeviseId()));
        } else {
            devise = deviseRepository.findByCode("XAF")
                    .orElseThrow(() -> new ResourceNotFoundException("Devise XAF non trouvée."));
        }

        // 4. Valider le moyen de paiement
        try {
            MoyenPaiement.valueOf(request.getMoyenPaiement().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Moyen de paiement invalide : " + request.getMoyenPaiement()
                    + ". Valeurs : ESPECES, CHEQUE, VIREMENT, CARTE_BANCAIRE, ORANGE_MONEY");
        }

        // 5. Créer l'encaissement
        Encaissement encaissement = encaissementMapper.toEntity(request, client, compte, devise);
        encaissement.setNumero(genererNumero("ENC"));
        encaissement.setDateEncaissement(LocalDate.now());

        // Récupérer l'utilisateur connecté
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur saisiPar = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));
        encaissement.setSaisiPar(saisiPar);

        // 6. Créditer le compte financier (simulation)
        compte.setSolde(compte.getSolde().add(request.getMontant()));
        compteFinancierRepository.save(compte);

        // 7. Sauvegarder
        Encaissement saved = encaissementRepository.save(encaissement);

        // 8. Si des affectations de factures sont fournies, les traiter
        if (request.getAffectations() != null && !request.getAffectations().isEmpty()) {
            affecter(saved.getId(), request.getAffectations());
        }

        // 9. Audit
        journalAuditService.enregistrer(
                "CREATE", "Encaissement", saved.getId(),
                null,
                "Encaissement " + saved.getNumero() + " de " + saved.getMontant() + " XAF via " + saved.getMoyenPaiement(),
                null);

        return encaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // TROUVER PAR ID
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public EncaissementResponse findById(Long id) {
        Encaissement encaissement = trouverOuException(id);
        if (encaissement.getClient() != null) {
            encaissement.getClient().getRaisonSociale();
        }
        return encaissementMapper.toResponse(encaissement);
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER TOUS
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<EncaissementResponse> findAll() {
        List<Encaissement> encaissements = encaissementRepository.findAll();
        for (Encaissement e : encaissements) {
            if (e.getClient() != null) e.getClient().getRaisonSociale();
        }
        return encaissements.stream().map(encaissementMapper::toResponse).collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // AFFECTER UN ENCAISSEMENT À DES FACTURES
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void affecter(Long encaissementId, List<AffectationRequest> affectations) {
        Encaissement encaissement = trouverOuException(encaissementId);
        BigDecimal totalAffecte = BigDecimal.ZERO;

        for (AffectationRequest aff : affectations) {
            Facture facture = factureRepository.findById(aff.getFactureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable ID : " + aff.getFactureId()));

            if (facture.getStatut() != StatutFacture.VALIDEE && facture.getStatut() != StatutFacture.PARTIELLEMENT_PAYEE) {
                throw new WorkflowException("La facture " + facture.getNumero() + " n'est pas en statut VALIDEE ou PARTIELLEMENT_PAYEE.");
            }

            totalAffecte = totalAffecte.add(aff.getMontantAffecte());

            // Mettre à jour le statut de la facture
            BigDecimal restant = facture.getMontantTtc().subtract(aff.getMontantAffecte());
            if (restant.compareTo(BigDecimal.ZERO) <= 0) {
                facture.setStatut(StatutFacture.SOLDEE);
            } else {
                facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
            }
            factureRepository.save(facture);
        }

        // Vérifier que le total affecté ne dépasse pas le montant de l'encaissement
        if (totalAffecte.compareTo(encaissement.getMontant()) > 0) {
            throw new WorkflowException("Le total affecté (" + totalAffecte + ") dépasse le montant de l'encaissement (" + encaissement.getMontant() + ").");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ────────────────────────────────────────────────────────────────────────
    private Encaissement trouverOuException(Long id) {
        return encaissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encaissement introuvable avec ID : " + id));
    }

    private String genererNumero(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + "-" + LocalDate.now().toString().replace("-", "") + "-" + suffix;
    }
}
