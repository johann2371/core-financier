package com.corefi.service.impl;

import com.corefi.entity.JournalAudit;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JournalAuditServiceImpl implements IJournalAuditService {

    private final JournalAuditRepository journalAuditRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String entite, Long entiteId, String anciennesValeurs,
            String nouvellesValeurs, String adresseIp) {
        
        JournalAudit audit = new JournalAudit();
        audit.setAction(action);
        audit.setEntite(entite);
        audit.setEntiteId(entiteId);
        audit.setAnciennesValeurs(anciennesValeurs);
        audit.setNouvellesValeurs(nouvellesValeurs);
        audit.setAdresseIp(adresseIp);

        // Récupérer l'utilisateur courant s'il existe
        String email = SecurityContextHolder.getContext().getAuthentication() != null 
                ? SecurityContextHolder.getContext().getAuthentication().getName() 
                : "SYSTEM";
        
        if (!"anonymousUser".equals(email) && !"SYSTEM".equals(email)) {
            utilisateurRepository.findByEmail(email).ifPresent(audit::setUtilisateur);
        }

        journalAuditRepository.save(audit);
    }

    @Override
    public Page<JournalAudit> findAll(Pageable pageable) {
        return journalAuditRepository.findAllByOrderByDateActionDesc(pageable);
    }
}
