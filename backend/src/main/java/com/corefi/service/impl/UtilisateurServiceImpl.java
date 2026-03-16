package com.corefi.service.impl;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.entity.Utilisateur;
import com.corefi.service.interfaces.IUtilisateurService;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements IUtilisateurService {

    private final IJournalAuditService journalAuditService; // Audit injecté

    @Override
    @Transactional
    public Utilisateur creer(UtilisateurCreateRequest request) {
        return null;
    }

    @Override
    @Transactional
    public Utilisateur mettreAJour(Long id, UtilisateurCreateRequest request) {
        return null;
    }

    @Override
    public Utilisateur findById(Long id) {
        return null;
    }

    @Override
    public List<Utilisateur> findAll() {
        return null;
    }

    @Override
    @Transactional
    public void desactiver(Long id) {
    }
}
