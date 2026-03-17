package com.corefi.service.impl;

import com.corefi.dto.request.auth.LoginRequest;
import com.corefi.dto.response.auth.AuthResponse;
import com.corefi.entity.Utilisateur;
import com.corefi.exception.AccesNonAutoriseException;
import com.corefi.mapper.UtilisateurMapper;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.security.JwtTokenProvider;
import com.corefi.service.interfaces.IAuthService;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UtilisateurMapper utilisateurMapper;
    private final IJournalAuditService journalAuditService;

    private static final int MAX_TENTATIVES = 5;
    private static final int DUREE_BLOCAGE_MINUTES = 30;

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 1 : IAuthService.login() → définie dans l'interface, implémentée ici
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Vérifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccesNonAutoriseException("Identifiants incorrects"));

        // 2. Vérifier si le compte est bloqué (règle AGENTS.md : blocage 30 min après 5 échecs)
        if (utilisateur.getBloqueJusqua() != null
                && utilisateur.getBloqueJusqua().isAfter(LocalDateTime.now())) {
            throw new AccesNonAutoriseException(
                    "Compte bloqué jusqu'au " + utilisateur.getBloqueJusqua()
                            + ". Réessayez plus tard.");
        }

        // 3. Vérifier si le compte est actif
        if (!utilisateur.isActif()) {
            throw new AccesNonAutoriseException("Ce compte a été désactivé par l'administrateur.");
        }

        // 4. Déléguer à Spring Security pour la vérification du mot de passe BCrypt
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));

            // 5. Succès → réinitialiser les tentatives et mettre à jour dernier accès
            utilisateur.setTentativesConnexion(0);
            utilisateur.setBloqueJusqua(null);
            utilisateur.setDernierAcces(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);

            // 6. Générer les tokens JWT
            String token = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(utilisateur.getEmail());

            // 7. Audit (règle AGENTS.md : chaque connexion doit être tracée)
            journalAuditService.enregistrer(
                    "LOGIN", "Utilisateur", utilisateur.getId(),
                    null, "Connexion réussie depuis l'application", null);

            return utilisateurMapper.toAuthResponse(utilisateur, token, refreshToken);

        } catch (BadCredentialsException e) {
            // 8. Échec → incrémenter le compteur
            int tentatives = utilisateur.getTentativesConnexion() + 1;
            utilisateur.setTentativesConnexion(tentatives);

            if (tentatives >= MAX_TENTATIVES) {
                // Bloquer le compte 30 minutes
                utilisateur.setBloqueJusqua(
                        LocalDateTime.now().plusMinutes(DUREE_BLOCAGE_MINUTES));
                utilisateurRepository.save(utilisateur);
                throw new AccesNonAutoriseException(
                        "Compte bloqué pour " + DUREE_BLOCAGE_MINUTES + " minutes (5 tentatives échouées).");
            }

            utilisateurRepository.save(utilisateur);
            throw new AccesNonAutoriseException(
                    "Identifiants incorrects (" + tentatives + "/" + MAX_TENTATIVES + " tentatives).");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ÉTAPE 2 : IAuthService.refreshToken() → définie dans l'interface, implémentée ici
    // ────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // 1. Valider le refresh token
        if (!jwtTokenProvider.validateJwtToken(refreshToken)) {
            throw new AccesNonAutoriseException("Refresh token invalide ou expiré.");
        }

        // 2. Extraire l'email
        String email = jwtTokenProvider.getUserNameFromJwtToken(refreshToken);
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new AccesNonAutoriseException("Utilisateur introuvable."));

        if (!utilisateur.isActif()) {
            throw new AccesNonAutoriseException("Ce compte a été désactivé.");
        }

        // 3. Générer de nouveaux tokens
        String newToken = jwtTokenProvider.generateTokenFromEmail(
                utilisateur.getEmail(), utilisateur.getRole().name());
        String newRefresh = jwtTokenProvider.generateRefreshToken(utilisateur.getEmail());

        return utilisateurMapper.toAuthResponse(utilisateur, newToken, newRefresh);
    }
}
