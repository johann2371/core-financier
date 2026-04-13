package com.corefi.service.impl;

import com.corefi.dto.request.encaissement.AffectationRequest;
import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.entity.*;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.TypeTiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.enums.StatutEncaissement;
import com.corefi.mapper.EncaissementMapper;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IEncaissementService;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.corefi.repository.AffectationPaiementRepository;
import com.corefi.entity.AffectationPaiement;

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
    private final SessionCaisseRepository sessionCaisseRepository;
    private final AffectationPaiementRepository affectationPaiementRepository;
    private final EncaissementMapper encaissementMapper;
    private final IJournalAuditService journalAuditService;
    private final com.corefi.service.interfaces.INotificationService notificationService;

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER UN ENCAISSEMENT
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public EncaissementResponse creer(EncaissementCreateRequest request) {
        // 1. Charger les entités de base
        Tiers client = tiersRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        if (client.getType() != TypeTiers.CLIENT) {
            throw new WorkflowException("Seuls les clients peuvent faire l'objet d'un encaissement.");
        }

        CompteFinancier compte = compteFinancierRepository.findById(request.getCompteFinancierId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte financier introuvable"));

        Devise devise = request.getDeviseId() != null 
                ? deviseRepository.findById(request.getDeviseId()).orElseThrow(() -> new ResourceNotFoundException("Devise introuvable"))
                : deviseRepository.findByCode("XAF").orElseThrow(() -> new ResourceNotFoundException("Devise XAF non configurée"));

        // 2. Valider le moyen de paiement
        MoyenPaiement moyen;
        try {
            moyen = MoyenPaiement.valueOf(request.getMoyenPaiement().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Moyen de paiement invalide : " + request.getMoyenPaiement());
        }

        // 3. Mapper l'entité
        Encaissement encaissement = encaissementMapper.toEntity(request, client, compte, devise);
        encaissement.setNumero(genererNumero("ENC"));
        encaissement.setDateEncaissement(LocalDate.now());

        Utilisateur saisiPar = getUtilisateurConnecte();
        encaissement.setSaisiPar(saisiPar);

        // 4. Logique conditionnelle selon le type de paiement
        if (moyen == MoyenPaiement.ESPECES) {
            SessionCaisse session = sessionCaisseRepository.findCurrentActiveSession(saisiPar.getId())
                    .orElseThrow(() -> new WorkflowException("Aucune session de caisse ouverte pour enregistrer des espèces."));
            
            if (!session.getCaisse().getId().equals(compte.getId())) {
                 throw new WorkflowException("Le compte choisi ne correspond pas à la caisse de votre session.");
            }
            
            encaissement.setSessionCaisse(session);
            encaissement.setStatut(StatutEncaissement.VALIDEE);
            
            // Impact immédiat sur le solde
            compte.setSolde(compte.getSolde().add(request.getMontant()));
            compteFinancierRepository.save(compte);
        } 
        else if (moyen == MoyenPaiement.CHEQUE) {
            encaissement.setStatut(StatutEncaissement.ATTENTE_COMPENSATION);
        } 
        else if (moyen == MoyenPaiement.VIREMENT) {
            encaissement.setStatut(StatutEncaissement.A_CONFIRMER);
        } 
        else {
            // Mobile Money ou Carte (Impact Net)
            encaissement.setStatut(StatutEncaissement.VALIDEE);
            compte.setSolde(compte.getSolde().add(encaissement.getMontantNet()));
            compteFinancierRepository.save(compte);
        }

        // 5. Mettre à jour le solde du tiers (baisse de sa créance)
        if (client.getSolde() == null) client.setSolde(BigDecimal.ZERO);
        client.setSolde(client.getSolde().subtract(request.getMontant()));
        tiersRepository.save(client);

        // 6. Sauvegarder
        Encaissement saved = encaissementRepository.save(encaissement);

        // 7. Traiter les affectations si présentes
        if (request.getAffectations() != null && !request.getAffectations().isEmpty()) {
            affecter(saved.getId(), request.getAffectations());
        }

        // 8. Audit & Notification
        journalAuditService.enregistrer("CREATE", "Encaissement", saved.getId(), null, 
                "Encaissement " + saved.getNumero() + " (" + saved.getMontant() + " XAF)", null);

        notificationService.creerEtEnvoyer("Nouvel encaissement", 
                "Un encaissement de " + saved.getMontant() + " a été saisi par " + saisiPar.getNom(), "RESPONSABLE_FINANCIER");

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
            Facture facture;
            if (aff.getFactureId() != null) {
                facture = factureRepository.findById(aff.getFactureId())
                        .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable ID : " + aff.getFactureId()));
            } else if (aff.getNumeroFacture() != null && !aff.getNumeroFacture().isBlank()) {
                facture = factureRepository.findByNumero(aff.getNumeroFacture())
                        .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec le numéro : " + aff.getNumeroFacture()));
            } else {
                throw new WorkflowException("ID ou Numéro de facture obligatoire pour l'affectation.");
            }

            // Vérifier que la facture appartient bien au client de l'encaissement
            if (!facture.getTiers().getId().equals(encaissement.getClient().getId())) {
                throw new WorkflowException("La facture " + facture.getNumero() + " n'appartient pas au client " + encaissement.getClient().getRaisonSociale());
            }

            if (facture.getStatut() != StatutFacture.EN_ATTENTE_PAIEMENT && 
                facture.getStatut() != StatutFacture.VALIDEE && 
                facture.getStatut() != StatutFacture.PARTIELLEMENT_PAYEE) {
                throw new WorkflowException("La facture " + facture.getNumero() + " n'est pas en statut EN ATTENTE PAIEMENT ou PARTIELLEMENT PAYEE.");
            }

            totalAffecte = totalAffecte.add(aff.getMontantAffecte());

            // 1. Créer l'affectation de paiement
            AffectationPaiement affectation = new AffectationPaiement();
            affectation.setTransactionId(encaissement.getId());
            affectation.setTransactionType("ENCAISSEMENT");
            affectation.setFacture(facture);
            affectation.setMontantAffecte(aff.getMontantAffecte());
            affectation.setAffectePar(getUtilisateurConnecte());
            affectationPaiementRepository.save(affectation);

            // 2. Calculer le total déjà payé pour cette facture (incluant la nouvelle affectation)
            BigDecimal totalPaye = affectationPaiementRepository.findByFactureId(facture.getId()).stream()
                    .map(AffectationPaiement::getMontantAffecte)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. Mettre à jour le statut de la facture
            BigDecimal restant = facture.getMontantTtc().subtract(totalPaye);
            if (restant.compareTo(BigDecimal.ZERO) <= 0) {
                facture.setStatut(StatutFacture.SOLDEE);
            } else {
                facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
            }
            factureRepository.save(facture);
            
            // Note: La mise à jour du solde tiers est désormais faite globalement à la création de l'encaissement
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

    private Utilisateur getUtilisateurConnecte() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));
    }
}
