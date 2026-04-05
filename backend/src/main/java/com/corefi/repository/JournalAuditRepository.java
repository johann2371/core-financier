package com.corefi.repository;

import com.corefi.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {
    
    @EntityGraph(attributePaths = {"utilisateur"})
    Page<JournalAudit> findAllByOrderByDateActionDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("SELECT j FROM JournalAudit j WHERE " +
           "(:action IS NULL OR :action = '' OR j.action = :action) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(j.entite) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.utilisateur.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.utilisateur.prenom) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<JournalAudit> searchAudit(@Param("search") String search, @Param("action") String action, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    Page<JournalAudit> findByUtilisateurIdOrderByDateActionDesc(Long utilisateurId, Pageable pageable);
}
