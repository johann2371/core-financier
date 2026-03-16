package com.corefi.service.interfaces;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.entity.Utilisateur;
import java.util.List;

public interface IUtilisateurService {
    Utilisateur creer(UtilisateurCreateRequest request);

    Utilisateur mettreAJour(Long id, UtilisateurCreateRequest request);

    Utilisateur findById(Long id);

    List<Utilisateur> findAll();

    void desactiver(Long id);
}
