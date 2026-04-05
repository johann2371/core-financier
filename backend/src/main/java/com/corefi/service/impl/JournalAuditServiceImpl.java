package com.corefi.service.impl;

import com.corefi.entity.JournalAudit;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

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

        // Récupérer automatiquement l'adresse IP si non fournie
        if (adresseIp == null || adresseIp.isBlank()) {
            adresseIp = getClientIp();
        }
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

    /**
     * Récupère l'adresse IP du client depuis la requête HTTP courante.
     * Gère les proxys inverses via le header X-Forwarded-For.
     */
    private String getClientIp() {
        try {
            if (httpServletRequest == null) return null;
            
            // Vérifier les headers de proxy
            String ip = httpServletRequest.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            
            ip = httpServletRequest.getHeader("X-Real-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Page<JournalAudit> findAll(String search, String action, Pageable pageable) {
        if ((search == null || search.trim().isEmpty()) && (action == null || action.trim().isEmpty())) {
            return journalAuditRepository.findAllByOrderByDateActionDesc(pageable);
        }
        return journalAuditRepository.searchAudit(search, action, pageable);
    }
}


