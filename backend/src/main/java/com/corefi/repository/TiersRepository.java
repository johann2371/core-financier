package com.corefi.repository;

import com.corefi.entity.Tiers;
import com.corefi.enums.TypeTiers;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TiersRepository extends JpaRepository<Tiers, Long> {
    Optional<Tiers> findByCode(String code);

    List<Tiers> findByType(TypeTiers type);

    boolean existsByCode(String code);

    List<Tiers> findByRaisonSocialeContainingIgnoreCaseOrCodeContainingIgnoreCase(String raisonSociale, String code);
}
