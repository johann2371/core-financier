package com.corefi.service.impl;

import com.corefi.dto.request.sessioncaisse.SessionCaisseFermetureRequest;
import com.corefi.dto.request.sessioncaisse.SessionCaisseRequest;
import com.corefi.dto.response.sessioncaisse.SessionCaisseResponse;
import com.corefi.entity.*;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutSessionCaisse;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.ISessionCaisseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionCaisseServiceImpl implements ISessionCaisseService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EncaissementRepository encaissementRepository;
    private final DecaissementRepository decaissementRepository;
    private final IJournalAuditService journalAuditService;

    @Override
    @Transactional
    public SessionCaisseResponse ouvrirSession(SessionCaisseRequest request) {
        Utilisateur caissier = getCurrentUser();

        // 1. Vérifier si une session est déjà ouverte pour ce caissier
        sessionCaisseRepository.findCurrentActiveSession(caissier.getId())
                .ifPresent(s -> {
                    throw new WorkflowException("Vous avez déjà une session de caisse ouverte (ID: " + s.getId() + ").");
                });

        // 2. Vérifier la caisse
        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        // 3. Créer la session
        SessionCaisse session = new SessionCaisse();
        session.setCaisse(caisse);
        session.setCaissier(caissier);
        session.setDateOuverture(LocalDateTime.now());
        session.setSoldeInitial(request.getSoldeInitial());
        session.setStatut(StatutSessionCaisse.OUVERTE);

        SessionCaisse saved = sessionCaisseRepository.save(session);

        // Audit
        journalAuditService.enregistrer("CREATE", "SessionCaisse", saved.getId(),
                null, "Ouverture de session de caisse avec un solde initial de " + request.getSoldeInitial(), null);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SessionCaisseResponse fermerSession(Long sessionId, SessionCaisseFermetureRequest request) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable"));

        if (session.getStatut() == StatutSessionCaisse.FERMEE) {
            throw new WorkflowException("Cette session est déjà fermée.");
        }

        // 1. Calcul du solde théorique
        BigDecimal totalEncaissements = encaissementRepository.findBySessionCaisseId(sessionId).stream()
                .filter(e -> e.getMoyenPaiement() == MoyenPaiement.ESPECES)
                .map(Encaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDecaissements = decaissementRepository.findBySessionCaisseId(sessionId).stream()
                .filter(d -> d.getMoyenPaiement() == MoyenPaiement.ESPECES)
                .map(Decaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeTheorique = session.getSoldeInitial().add(totalEncaissements).subtract(totalDecaissements);

        // 2. Gestion de l'écart
        BigDecimal reel = request.getSoldeFinalReel();
        BigDecimal ecart = reel.subtract(soldeTheorique);

        if (ecart.compareTo(BigDecimal.ZERO) != 0 && (request.getMotifEcart() == null || request.getMotifEcart().isBlank())) {
            throw new WorkflowException("Un motif est obligatoire lorsqu'un écart de caisse est constaté (" + ecart + ").");
        }

        // 3. Mise à jour de la session
        session.setDateFermeture(LocalDateTime.now());
        session.setSoldeFinalTheorique(soldeTheorique);
        session.setSoldeFinalReel(reel);
        session.setEcart(ecart);
        session.setMotifEcart(request.getMotifEcart());
        session.setStatut(StatutSessionCaisse.FERMEE);

        SessionCaisse saved = sessionCaisseRepository.save(session);

        // Audit
        journalAuditService.enregistrer("UPDATE", "SessionCaisse", saved.getId(),
                "Theorique=" + soldeTheorique, "Reel=" + reel + ", Ecart=" + ecart + ", Motif=" + request.getMotifEcart(), null);

        return mapToResponse(saved);
    }

    @Override
    public Optional<SessionCaisseResponse> getSessionActiveCurrentCaissier() {
        Utilisateur user = getCurrentUser();
        return sessionCaisseRepository.findCurrentActiveSession(user.getId())
                .map(this::mapToResponse);
    }

    @Override
    public List<SessionCaisseResponse> getHistoriqueSessionsCaissier(Long caissierId) {
        return sessionCaisseRepository.findByCaissierIdOrderByDateOuvertureDesc(caissierId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionCaisseResponse> getHistoriqueSessionsCurrentCaissier() {
        Utilisateur user = getCurrentUser();
        return sessionCaisseRepository.findByCaissierIdOrderByDateOuvertureDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionCaisseResponse> getAllHistoriqueSessions() {
        return sessionCaisseRepository.findAll().stream()
                .sorted((a, b) -> b.getDateOuverture().compareTo(a.getDateOuverture()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SessionCaisseResponse getById(Long id) {
        return sessionCaisseRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable"));
    }

    private Utilisateur getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new WorkflowException("Utilisateur non authentifié"));
    }

    private SessionCaisseResponse mapToResponse(SessionCaisse s) {
        SessionCaisseResponse r = new SessionCaisseResponse();
        r.setId(s.getId());
        r.setCaisseId(s.getCaisse().getId());
        r.setCaisseNom(s.getCaisse().getLibelle());
        r.setCaissierNom(s.getCaissier().getNom() + " " + s.getCaissier().getPrenom());
        r.setDateOuverture(s.getDateOuverture());
        r.setDateFermeture(s.getDateFermeture());
        r.setSoldeInitial(s.getSoldeInitial());
        r.setSoldeFinalTheorique(s.getSoldeFinalTheorique());
        r.setSoldeFinalReel(s.getSoldeFinalReel());
        r.setEcart(s.getEcart());
        r.setMotifEcart(s.getMotifEcart());
        r.setStatut(s.getStatut());
        return r;
    }
}
