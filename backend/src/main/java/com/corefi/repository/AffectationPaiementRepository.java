package com.corefi.repository;

import com.corefi.entity.AffectationPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AffectationPaiementRepository extends JpaRepository<AffectationPaiement, Long> {
    List<AffectationPaiement> findByFactureId(Long factureId);

    List<AffectationPaiement> findByTransactionIdAndTransactionType(Long transactionId, String transactionType);
}
