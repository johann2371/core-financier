package com.corefi.repository;

import com.corefi.entity.Facture;
import com.corefi.enums.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumero(String numero);

    List<Facture> findByStatut(StatutFacture statut);

    List<Facture> findByTiersId(Long tiersId);
}
