package com.corefi.repository;

import com.corefi.entity.LigneReleveBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LigneReleveBancaireRepository extends JpaRepository<LigneReleveBancaire, Long> {
    List<LigneReleveBancaire> findByRapprochementId(Long rapprochementId);
    List<LigneReleveBancaire> findByRapprochementIdAndMatchedFalse(Long rapprochementId);
}
