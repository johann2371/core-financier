package com.corefi.service.impl;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.entity.Facture;
import com.corefi.entity.Tiers;
import com.corefi.entity.Utilisateur;
import com.corefi.enums.StatutFacture;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.FactureMapper;
import com.corefi.repository.FactureRepository;
import com.corefi.repository.TiersRepository;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.repository.DeviseRepository;
import com.corefi.repository.AffectationPaiementRepository;
import com.corefi.service.interfaces.IFactureService;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.entity.AffectationPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FactureServiceImpl implements IFactureService {

    private final FactureRepository factureRepository;
    private final TiersRepository tiersRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DeviseRepository deviseRepository;
    private final AffectationPaiementRepository affectationPaiementRepository;
    private final FactureMapper factureMapper;
    private final IJournalAuditService journalAuditService;
    private final com.corefi.service.interfaces.INotificationService notificationService;

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER UNE FACTURE
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FactureResponse creer(FactureCreateRequest request) {
        // 1. Vérifier le tiers
        Tiers tiers = tiersRepository.findById(request.getTiersId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tiers introuvable avec l'ID : " + request.getTiersId()));

        if (!tiers.isActif()) {
            throw new WorkflowException("Impossible de créer une facture pour un tiers désactivé.");
        }

        // Vérifier la cohérence Type Tiers / Type Facture
        if (request.getType().equalsIgnoreCase("VENTE") && tiers.getType().name().equals("FOURNISSEUR")) {
            throw new WorkflowException("Une facture de VENTE doit être associée à un CLIENT.");
        }
        if (request.getType().equalsIgnoreCase("ACHAT") && tiers.getType().name().equals("CLIENT")) {
            throw new WorkflowException("Une facture d'ACHAT doit être associée à un FOURNISSEUR.");
        }

        // 2. Transformer la requête en entité (FactureMapper calcule la TVA et les
        // totaux)
        Facture facture = factureMapper.toEntity(request, tiers);

        // 3. Générer le numéro de facture (ex: FAC-20231025-0001)
        facture.setNumero(genererNumeroFacture());
        facture.setDateFacture(LocalDate.now());

        // Délai de paiement de 30 jours par défaut si non spécifié
        facture.setDateEcheance(LocalDate.now().plusDays(30));

        // 4. Récupérer l'utilisateur connecté (celui qui crée la facture)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur creePar = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));
        facture.setCreePar(creePar);

        // 5. Devise par défaut (XAF)
        facture.setDevise(deviseRepository.findByCode("XAF")
                .orElseThrow(() -> new ResourceNotFoundException("Devise XAF non trouvée en base.")));

        // 6. Statut direct à VALIDEE (pour refléter la dette immédiatement)
        facture.setStatut(StatutFacture.EN_ATTENTE_PAIEMENT);
        
        // 6. Mettre à jour le solde du tiers
        if (tiers.getSolde() == null) tiers.setSolde(java.math.BigDecimal.ZERO);
        if (tiers.getTotalDette() == null) tiers.setTotalDette(java.math.BigDecimal.ZERO);

        tiers.setSolde(tiers.getSolde().add(facture.getMontantTtc()));
        tiers.setTotalDette(tiers.getTotalDette().add(facture.getMontantTtc()));
        tiersRepository.save(tiers);

        // 8. Sauvegarder
        Facture saved = factureRepository.save(facture);

        // 6. Audit
        journalAuditService.enregistrer(
                "CREATE", "Facture", saved.getId(),
                null,
                "Création facture " + saved.getNumero() + " pour " + tiers.getRaisonSociale() + " (TTC: "
                        + saved.getMontantTtc() + ")",
                null);

        // 7. Notification
        notificationService.creerEtEnvoyer(
                "Nouvelle facture créée",
                creePar.getPrenom() + " " + creePar.getNom() + " a créé la facture " + saved.getNumero() + " pour " + tiers.getRaisonSociale() + " — " + saved.getMontantTtc() + " XAF.",
                "RESPONSABLE_FINANCIER");

        return factureMapper.toResponse(saved, saved.getMontantTtc());
    }

    // ────────────────────────────────────────────────────────────────────────
    // TROUVER PAR ID
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public FactureResponse findById(Long id) {
        Facture facture = trouverOuException(id);
        
        // Initialise manuellement les entités Lazy pour éviter LazyInitializationException dans FactureMapper
        if (facture.getTiers() != null) {
            facture.getTiers().getRaisonSociale();
        }
        if (facture.getLignes() != null) {
            facture.getLignes().size();
        }

        return factureMapper.toResponse(facture, calculerResteAPayer(facture));
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER TOUTES LES FACTURES
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<FactureResponse> findAll() {
        List<Facture> factures = factureRepository.findAll();
        
        // Initialise manuellement les entités Lazy
        for (Facture f : factures) {
            if (f.getTiers() != null) {
                f.getTiers().getRaisonSociale();
            }
        }

        return factures.stream()
                .map(f -> factureMapper.toResponse(f, calculerResteAPayer(f)))
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // VALIDER UNE FACTURE
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FactureResponse valider(Long id) {
        Facture facture = trouverOuException(id);

        if (facture.getStatut() != StatutFacture.BROUILLON) {
            throw new WorkflowException("Seule une facture en statut BROUILLON peut être validée.");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur validePar = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable."));

        facture.setStatut(StatutFacture.EN_ATTENTE_PAIEMENT);
        facture.setValidePar(validePar);

        // Ajout au solde du Tiers
        Tiers tiers = facture.getTiers();
        if (tiers.getSolde() == null) tiers.setSolde(java.math.BigDecimal.ZERO);
        tiers.setSolde(tiers.getSolde().add(facture.getMontantTtc()));
        tiersRepository.save(tiers);

        Facture saved = factureRepository.save(facture);

        journalAuditService.enregistrer(
                "UPDATE", "Facture", saved.getId(),
                "statut=BROUILLON",
                "statut=VALIDEE (par " + validePar.getNom() + ")",
                null);

        return factureMapper.toResponse(saved, saved.getMontantTtc());
    }

    // ────────────────────────────────────────────────────────────────────────
    // ANNULER UNE FACTURE
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FactureResponse annuler(Long id) {
        Facture facture = trouverOuException(id);

        if (facture.getStatut() == StatutFacture.SOLDEE) {
            throw new WorkflowException("Impossible d'annuler une facture qui est déjà soldée.");
        }
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new WorkflowException("La facture " + facture.getNumero() + " est déjà annulée.");
        }

        String ancienStatut = facture.getStatut().name();
        
        // Si la facture était déjà validée, on annule son impact sur le solde du tiers
        if (facture.getStatut() == StatutFacture.EN_ATTENTE_PAIEMENT || facture.getStatut() == StatutFacture.PARTIELLEMENT_PAYEE) {
            Tiers tiers = facture.getTiers();
            if (tiers.getSolde() != null) {
                tiers.setSolde(tiers.getSolde().subtract(facture.getMontantTtc()));
                tiersRepository.save(tiers);
            }
        }
        
        facture.setStatut(StatutFacture.ANNULEE);

        Facture saved = factureRepository.save(facture);

        journalAuditService.enregistrer(
                "UPDATE", "Facture", saved.getId(),
                "statut=" + ancienStatut,
                "statut=ANNULEE",
                null);

        return factureMapper.toResponse(saved, calculerResteAPayer(saved));
    }

    // ────────────────────────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ────────────────────────────────────────────────────────────────────────
    private Facture trouverOuException(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec ID : " + id));
    }

    private String genererNumeroFacture() {
        // Logique de génération basique : FAC-YYYYMMDD-XXXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = factureRepository.count() + 1; // S'assure de l'unicité simple (en env de prod, utiliser une
                                                    // séquence BD)
        return String.format("FAC-%s-%04d", datePart, count);
    }

    private BigDecimal calculerResteAPayer(Facture f) {
        BigDecimal totalPaye = affectationPaiementRepository.findByFactureId(f.getId()).stream()
                .map(AffectationPaiement::getMontantAffecte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return f.getMontantTtc().subtract(totalPaye);
    }
}
