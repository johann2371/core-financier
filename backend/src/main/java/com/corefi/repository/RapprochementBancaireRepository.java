package com.corefi.repository;

import com.corefi.entity.RapprochementBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RapprochementBancaireRepository extends JpaRepository<RapprochementBancaire, Long> {
    List<RapprochementBancaire> findByCompteFinancierIdOrderByDateFinDesc(Long compteId);
}
