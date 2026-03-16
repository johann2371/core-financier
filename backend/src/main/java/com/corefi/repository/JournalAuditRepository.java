package com.corefi.repository;

import com.corefi.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {
    Page<JournalAudit> findAllByOrderByDateActionDesc(Pageable pageable);

    Page<JournalAudit> findByUtilisateurIdOrderByDateActionDesc(Long utilisateurId, Pageable pageable);
}
