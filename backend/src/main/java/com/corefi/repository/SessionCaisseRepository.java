package com.corefi.repository;

import com.corefi.entity.SessionCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    @Query("SELECT s FROM SessionCaisse s WHERE s.caissier.id = :caissierId AND s.statut = 'OUVERTE'")
    Optional<SessionCaisse> findCurrentActiveSession(@Param("caissierId") Long caissierId);

    List<SessionCaisse> findByCaissierIdOrderByDateOuvertureDesc(Long caissierId);
}
