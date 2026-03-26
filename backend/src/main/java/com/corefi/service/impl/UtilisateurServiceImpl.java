package com.corefi.service.impl;

import com.corefi.dto.request.utilisateur.ProfileUpdateRequest;
import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import com.corefi.entity.Utilisateur;
import com.corefi.enums.Role;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.UtilisateurMapper;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.IUtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements IUtilisateurService {

    // ── Dépendances injectées via les INTERFACES (règle AGENTS.md) ──────────
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurMapper utilisateurMapper;
    private final IJournalAuditService journalAuditService; // interface ✅

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER UN UTILISATEUR
    // Seul l'ADMINISTRATEUR peut accéder à cette méthode (@PreAuthorize dans le controller)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public UtilisateurResponse creer(UtilisateurCreateRequest request) {
        // 1. Vérifier que l'email n'est pas déjà utilisé
        if (utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new WorkflowException("Un utilisateur avec l'email '"
                    + request.getEmail() + "' existe déjà.");
        }

        // 2. Valider le rôle
        Role role;
        try {
            role = Role.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Rôle invalide : " + request.getRole()
                    + ". Valeurs acceptées : COMPTABLE, RESPONSABLE_FINANCIER, PDG, CAISSIER, ADMINISTRATEUR.");
        }

        // 3. Construire l'entité via le mapper
        Utilisateur utilisateur = utilisateurMapper.toEntity(request);
        utilisateur.setRole(role);

        // 4. Générer un mot de passe temporaire si non fourni
        String motPasseClair = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : genererMotDePasseTemporaire();

        utilisateur.setPassword(passwordEncoder.encode(motPasseClair));
        utilisateur.setDateCreation(LocalDateTime.now());
        utilisateur.setActif(true);

        // 5. Sauvegarder
        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // 6. Audit (règle AGENTS.md : toute création doit être tracée)
        journalAuditService.enregistrer(
                "CREATE", "Utilisateur", saved.getId(),
                null,
                "Création de l'utilisateur " + saved.getEmail() + " avec le rôle " + saved.getRole(),
                null
        );

        return utilisateurMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // METTRE À JOUR UN UTILISATEUR
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public UtilisateurResponse mettreAJour(Long id, UtilisateurCreateRequest request) {
        // 1. Trouver l'utilisateur
        Utilisateur utilisateur = trouverOuExcepion(id);

        // 2. Vérifier si le nouvel email est déjà pris par un AUTRE utilisateur
        utilisateurRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> { throw new WorkflowException(
                        "L'email '" + request.getEmail() + "' est déjà utilisé par un autre compte."); });

        // 3. Mettre à jour les champs
        String anciennesValeurs = "email=" + utilisateur.getEmail()
                + ", role=" + utilisateur.getRole();

        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setRole(Role.valueOf(request.getRole()));

        // Mise à jour du mot de passe uniquement si fourni
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // 4. Audit
        journalAuditService.enregistrer(
                "UPDATE", "Utilisateur", saved.getId(),
                anciennesValeurs,
                "email=" + saved.getEmail() + ", role=" + saved.getRole(),
                null
        );

        return utilisateurMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // TROUVER PAR ID
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public UtilisateurResponse findById(Long id) {
        return utilisateurMapper.toResponse(trouverOuExcepion(id));
    }

    // ────────────────────────────────────────────────────────────────────────
    // LISTER TOUS LES UTILISATEURS
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public List<UtilisateurResponse> findAll() {
        return utilisateurRepository.findAll()
                .stream()
                .map(utilisateurMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // DÉSACTIVER UN UTILISATEUR (soft delete)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void desactiver(Long id) {
        Utilisateur utilisateur = trouverOuExcepion(id);

        // On ne désactive pas l'admin système
        if (utilisateur.getEmail().equals("admin@corefi.cm")) {
            throw new WorkflowException("Le compte administrateur système ne peut pas être désactivé.");
        }

        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);

        // Audit
        journalAuditService.enregistrer(
                "DELETE", "Utilisateur", id,
                "actif=true",
                "actif=false (désactivation)",
                null
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // METTRE À JOUR LE PROFIL (Utilisateur connecté)
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public UtilisateurResponse updateProfile(String email, ProfileUpdateRequest request) {
        // 1. Trouver l'utilisateur par son email (issu du SecurityContext)
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));

        // 2. Vérifier si le nouvel email est disponible (si changé)
        if (!utilisateur.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new WorkflowException("L'email '" + request.getEmail() + "' est déjà utilisé.");
            }
        }

        // 3. Mettre à jour les infos
        String anciennesValeurs = "nom=" + utilisateur.getNom() + ", prenom=" + utilisateur.getPrenom() + ", photo=" + utilisateur.getPhotoUrl();
        
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setPhotoUrl(request.getPhotoUrl());

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // 4. Audit
        journalAuditService.enregistrer(
                "UPDATE", "Utilisateur", saved.getId(),
                anciennesValeurs,
                "Profil mis à jour via interface utilisateur",
                null
        );

        return utilisateurMapper.toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ────────────────────────────────────────────────────────────────────────
    private Utilisateur trouverOuExcepion(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'id : " + id));
    }

    /** Génère un mot de passe temporaire aléatoire de 12 caractères */
    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
