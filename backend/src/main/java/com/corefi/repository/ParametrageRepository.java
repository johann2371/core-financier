package com.corefi.repository;

import com.corefi.entity.Parametrage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParametrageRepository extends JpaRepository<Parametrage, Long> {
    Optional<Parametrage> findByCle(String cle);
}
