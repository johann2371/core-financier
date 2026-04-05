package com.corefi.service.interfaces;

import com.corefi.dto.request.utilisateur.ProfileUpdateRequest;
import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import java.util.List;

public interface IUtilisateurService {
    UtilisateurResponse creer(UtilisateurCreateRequest request);
    UtilisateurResponse mettreAJour(Long id, UtilisateurCreateRequest request);
    UtilisateurResponse findById(Long id);
    List<UtilisateurResponse> findAll();
    void desactiver(Long id);
    void reactiver(Long id);
    UtilisateurResponse updateProfile(String email, ProfileUpdateRequest request);
}
