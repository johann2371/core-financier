package com.corefi.service.interfaces;

import com.corefi.entity.JournalAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IJournalAuditService {
    void enregistrer(String action, String entite, Long entiteId,
            String anciennesValeurs, String nouvellesValeurs, String adresseIp);

    Page<JournalAudit> findAll(String search, String action, Pageable pageable);
}
