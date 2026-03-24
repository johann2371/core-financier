package com.corefi.service.impl;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.entity.*;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.TypeTiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.DecaissementMapper;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IDecaissementService;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecaissementServiceImpl implements IDecaissementService {

    private final DecaissementRepository decaissementRepository;
    private final TiersRepository tiersRepository;
    private final DeviseRepository deviseRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DecaissementMapper decaissementMapper;
    private final IJournalAuditService journalAuditService;
    private final com.corefi.service.interfaces.INotificationService notificationService;

    // Seuil au-delà duquel l'approbation du PDG est requise (en XAF)
    private static final BigDecimal SEUIL_PDG = new BigDecimal("500000");

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 1 : COMPTABLE CRÉE LE DÉCAISSEMENT (statut → BROUILLON)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse creer(DecaissementCreateRequest request) {
        // Vérifier le fournisseur
        Tiers fournisseur = tiersRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable ID : " + request.getFournisseurId()));

        if (fournisseur.getType() != TypeTiers.FOURNISSEUR) {
            throw new WorkflowException("Un décaissement doit être associé à un FOURNISSEUR.");
        }

        // Devise (XAF par défaut)
        Devise devise = null;
        if (request.getDeviseId() != null) {
            devise = deviseRepository.findById(request.getDeviseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise introuvable."));
        } else {
            devise = deviseRepository.findByCode("XAF")
                    .orElseThrow(() -> new ResourceNotFoundException("Devise XAF non trouvée."));
        }

        // Créer l'entité
        Decaissement decaissement = decaissementMapper.toEntity(request, fournisseur, devise);
        decaissement.setNumero(genererNumero("DEC"));
        decaissement.setDateDecaissement(LocalDate.now());
        decaissement.setDateSaisie(LocalDateTime.now());

        // Vérifier si le seuil PDG est atteint
        decaissement.setSeuilPdgRequis(request.getMontant().compareTo(SEUIL_PDG) >= 0);

        // Utilisateur connecté
        Utilisateur saisiPar = getUtilisateurConnecte();
        decaissement.setSaisiPar(saisiPar);

        // Statut direct à EN_ATTENTE (plus de brouillon, visibilité immédiate sur Dashboard)
        decaissement.setStatut(StatutDecaissement.EN_ATTENTE);

        Decaissement saved = decaissementRepository.save(decaissement);

        journalAuditService.enregistrer(
                "CREATE", "Decaissement", saved.getId(), null,
                "Décaissement " + saved.getNumero() + " — " + saved.getMontant() + " XAF"
                        + (saved.isSeuilPdgRequis() ? " (seuil PDG requis)" : ""),
                null);

        // Notification immédiate pour le RF
        notificationService.creerEtEnvoyer(
                "Nouveau décaissement à valider",
                "Un nouveau décaissement " + saved.getNumero() + " de " + saved.getMontant() + " XAF a été créé et attend votre validation.",
                "RESPONSABLE_FINANCIER");

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 2 : COMPTABLE SOUMET LE DÉCAISSEMENT (BROUILLON → EN_ATTENTE)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse soumettre(Long id) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.BROUILLON) {
            throw new WorkflowException("Seul un décaissement en BROUILLON peut être soumis.");
        }

        d.setStatut(StatutDecaissement.EN_ATTENTE);
        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=BROUILLON", "statut=EN_ATTENTE", null);

        notificationService.creerEtEnvoyer(
                "Nouveau décaissement à valider",
                "Le décaissement " + d.getNumero() + " de " + d.getMontant() + " XAF a été soumis pour validation.",
                "RESPONSABLE_FINANCIER");

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 3a : RF VALIDE (EN_ATTENTE → VALIDEE_RF ou EN_ATTENTE_PDG)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse validerRF(Long id, ValidationRFRequest request) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.EN_ATTENTE) {
            throw new WorkflowException("Seul un décaissement EN_ATTENTE peut être validé par le RF.");
        }

        Utilisateur validePar = getUtilisateurConnecte();
        d.setValidePar(validePar);
        d.setDateValidation(LocalDateTime.now());

        if (d.isSeuilPdgRequis()) {
            // Le montant dépasse le seuil → transmis au PDG
            d.setStatut(StatutDecaissement.EN_ATTENTE_PDG);
        } else {
            // Pas de seuil → directement validé
            d.setStatut(StatutDecaissement.VALIDEE_RF);
        }

        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=EN_ATTENTE", "statut=" + saved.getStatut().name(), null);

        if (d.isSeuilPdgRequis()) {
            notificationService.creerEtEnvoyer(
                "Approbation requise",
                "Le décaissement " + d.getNumero() + " (" + d.getMontant() + " XAF) nécessite votre approbation PDG.",
                "PDG");
        } else {
            notificationService.creerEtEnvoyer(
                "Paiement à exécuter",
                "Le décaissement " + d.getNumero() + " a été validé et attend votre exécution en caisse.",
                "CAISSIER");
        }

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 3b : RF REJETTE
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse rejeterRF(Long id, ValidationRFRequest request) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.EN_ATTENTE) {
            throw new WorkflowException("Seul un décaissement EN_ATTENTE peut être rejeté par le RF.");
        }

        if (request.getMotif() == null || request.getMotif().isBlank()) {
            throw new WorkflowException("Le motif de rejet est obligatoire.");
        }

        d.setStatut(StatutDecaissement.REJETEE_RF);
        d.setMotifRejetRf(request.getMotif());
        d.setValidePar(getUtilisateurConnecte());
        d.setDateValidation(LocalDateTime.now());

        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=EN_ATTENTE", "statut=REJETEE_RF — motif: " + request.getMotif(), null);

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 4a : PDG APPROUVE (EN_ATTENTE_PDG → VALIDEE_PDG)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse approuverPDG(Long id, ApprobationPDGRequest request) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.EN_ATTENTE_PDG) {
            throw new WorkflowException("Seul un décaissement EN_ATTENTE_PDG peut être approuvé par le PDG.");
        }

        d.setStatut(StatutDecaissement.VALIDEE_PDG);
        d.setApprouveParPdg(getUtilisateurConnecte());
        d.setDateApprobationPdg(LocalDateTime.now());

        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=EN_ATTENTE_PDG", "statut=VALIDEE_PDG", null);

        notificationService.creerEtEnvoyer(
                "Paiement à exécuter",
                "Le décaissement " + d.getNumero() + " a été approuvé par le PDG et attend votre exécution en caisse.",
                "CAISSIER");

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 4b : PDG REJETTE
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse rejeterPDG(Long id, ApprobationPDGRequest request) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.EN_ATTENTE_PDG) {
            throw new WorkflowException("Seul un décaissement EN_ATTENTE_PDG peut être rejeté par le PDG.");
        }

        if (request.getMotif() == null || request.getMotif().isBlank()) {
            throw new WorkflowException("Le motif de rejet est obligatoire.");
        }

        d.setStatut(StatutDecaissement.REJETEE_PDG);
        d.setMotifRejetPdg(request.getMotif());
        d.setApprouveParPdg(getUtilisateurConnecte());
        d.setDateApprobationPdg(LocalDateTime.now());

        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=EN_ATTENTE_PDG", "statut=REJETEE_PDG — motif: " + request.getMotif(), null);

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 5 : CAISSIER EXÉCUTE LE PAIEMENT (VALIDEE_RF/VALIDEE_PDG → EXECUTEE)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DecaissementResponse executer(Long id, ExecutionCaissierRequest request) {
        Decaissement d = trouverOuException(id);

        if (d.getStatut() != StatutDecaissement.VALIDEE_RF && d.getStatut() != StatutDecaissement.VALIDEE_PDG) {
            throw new WorkflowException("Seul un décaissement VALIDEE_RF ou VALIDEE_PDG peut être exécuté.");
        }

        // Vérifier le compte financier
        CompteFinancier compte = compteFinancierRepository.findById(request.getCompteFinancierId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte financier introuvable."));

        // Vérifier le solde suffisant
        if (compte.getSolde().compareTo(d.getMontant()) < 0) {
            throw new WorkflowException("Solde insuffisant sur le compte " + compte.getNumero()
                    + ". Solde actuel : " + compte.getSolde() + " XAF, montant requis : " + d.getMontant() + " XAF.");
        }

        // Valider le moyen de paiement
        try {
            MoyenPaiement.valueOf(request.getMoyenPaiement().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Moyen de paiement invalide : " + request.getMoyenPaiement());
        }

        // Débiter le compte (simulation)
        compte.setSolde(compte.getSolde().subtract(d.getMontant()));
        compteFinancierRepository.save(compte);

        // Soustraire du solde fournisseur (on a payé notre dette)
        Tiers fournisseur = d.getFournisseur();
        if (fournisseur.getSolde() == null) fournisseur.setSolde(java.math.BigDecimal.ZERO);
        fournisseur.setSolde(fournisseur.getSolde().subtract(d.getMontant()));
        tiersRepository.save(fournisseur);

        // Mettre à jour le décaissement
        d.setStatut(StatutDecaissement.EXECUTEE);
        d.setMoyenPaiement(MoyenPaiement.valueOf(request.getMoyenPaiement().toUpperCase()));
        d.setCompteFinancier(compte);
        d.setReferenceExecution(request.getReferenceExecution());
        d.setExecutePar(getUtilisateurConnecte());
        d.setDateExecution(LocalDateTime.now());

        Decaissement saved = decaissementRepository.save(d);

        journalAuditService.enregistrer("UPDATE", "Decaissement", id,
                "statut=" + (d.isSeuilPdgRequis() ? "VALIDEE_PDG" : "VALIDEE_RF"),
                "statut=EXECUTEE — " + d.getMontant() + " XAF via " + d.getMoyenPaiement(),
                null);

        return decaissementMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // TROUVER PAR ID
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public DecaissementResponse findById(Long id) {
        Decaissement d = trouverOuException(id);
        if (d.getFournisseur() != null) d.getFournisseur().getRaisonSociale();
        return decaissementMapper.toResponse(d);
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER TOUS
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<DecaissementResponse> findAll() {
        List<Decaissement> list = decaissementRepository.findAll();
        for (Decaissement d : list) {
            if (d.getFournisseur() != null) d.getFournisseur().getRaisonSociale();
        }
        return list.stream().map(decaissementMapper::toResponse).collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ────────────────────────────────────────────────────────────────────────
    private Decaissement trouverOuException(Long id) {
        return decaissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Décaissement introuvable avec ID : " + id));
    }

    private Utilisateur getUtilisateurConnecte() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));
    }

    private String genererNumero(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + "-" + LocalDate.now().toString().replace("-", "") + "-" + suffix;
    }
}
