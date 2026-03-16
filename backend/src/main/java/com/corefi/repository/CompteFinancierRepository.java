package com.corefi.repository;

import com.corefi.entity.CompteFinancier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompteFinancierRepository extends JpaRepository<CompteFinancier, Long> {
    Optional<CompteFinancier> findByNumero(String numero);
}
