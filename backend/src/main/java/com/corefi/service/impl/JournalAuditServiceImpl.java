package com.corefi.service.impl;

import com.corefi.entity.JournalAudit;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalAuditServiceImpl implements IJournalAuditService {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Audit s'enregistre même si la transaction parente échoue
    public void enregistrer(String action, String entite, Long entiteId, String anciennesValeurs,
            String nouvellesValeurs, String adresseIp) {
    }

    @Override
    public Page<JournalAudit> findAll(Pageable pageable) {
        return null;
    }
}
