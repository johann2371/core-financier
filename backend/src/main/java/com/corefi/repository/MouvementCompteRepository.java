package com.corefi.repository;

import com.corefi.entity.MouvementCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MouvementCompteRepository extends JpaRepository<MouvementCompte, Long> {
    List<MouvementCompte> findByCompteFinancierIdOrderByDateOperationDesc(Long compteId);
}
