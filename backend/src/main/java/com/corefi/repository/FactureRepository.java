package com.corefi.repository;

import com.corefi.entity.Facture;
import com.corefi.enums.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumero(String numero);

    List<Facture> findByStatut(StatutFacture statut);

    List<Facture> findByTiersId(Long tiersId);

    /** Charge la facture avec ses lignes, tiers et devise en une seule requête (pour PDF) */
    @Query("SELECT f FROM Facture f LEFT JOIN FETCH f.lignes LEFT JOIN FETCH f.tiers LEFT JOIN FETCH f.devise WHERE f.id = :id")
    Optional<Facture> findByIdWithDetails(@Param("id") Long id);
}
