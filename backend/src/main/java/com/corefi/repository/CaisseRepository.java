package com.corefi.repository;

import com.corefi.entity.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    Optional<Caisse> findByResponsableId(Long responsableId);
}
