package com.corefi.mapper;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.auth.AuthResponse;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import com.corefi.entity.Utilisateur;
import com.corefi.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    /** Requête → Entité (pour la création) */
    public Utilisateur toEntity(UtilisateurCreateRequest request) {
        Utilisateur u = new Utilisateur();
        u.setNom(request.getNom());
        u.setPrenom(request.getPrenom());
        u.setEmail(request.getEmail());
        u.setRole(Role.valueOf(request.getRole()));
        u.setActif(true);
        return u;
    }

    /** Entité → UtilisateurResponse (pour le listing admin — SANS mot de passe) */
    public UtilisateurResponse toResponse(Utilisateur u) {
        UtilisateurResponse r = new UtilisateurResponse();
        r.setId(u.getId());
        r.setNom(u.getNom());
        r.setPrenom(u.getPrenom());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole().name());
        r.setPhotoUrl(u.getPhotoUrl());
        r.setActif(u.isActif());
        r.setDateCreation(u.getDateCreation());
        r.setDernierAcces(u.getDernierAcces());
        return r;
    }

    /** Entité → AuthResponse (utilisé après login) */
    public AuthResponse toAuthResponse(Utilisateur utilisateur, String token, String refreshToken) {
        return new AuthResponse(
                token,
                refreshToken,
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole().name(),
                utilisateur.getPhotoUrl()
        );
    }
}
