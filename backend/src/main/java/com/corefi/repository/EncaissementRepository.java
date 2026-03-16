package com.corefi.repository;

import com.corefi.entity.Encaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EncaissementRepository extends JpaRepository<Encaissement, Long> {
    Optional<Encaissement> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
