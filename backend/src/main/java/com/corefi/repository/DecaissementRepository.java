package com.corefi.repository;

import com.corefi.entity.Decaissement;
import com.corefi.enums.StatutDecaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DecaissementRepository extends JpaRepository<Decaissement, Long> {
    Optional<Decaissement> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Decaissement> findByStatut(StatutDecaissement statut);

    List<Decaissement> findByFournisseurId(Long fournisseurId);
}
