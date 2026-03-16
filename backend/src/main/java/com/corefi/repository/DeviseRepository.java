package com.corefi.repository;

import com.corefi.entity.Devise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviseRepository extends JpaRepository<Devise, Long> {
    Optional<Devise> findByCode(String code);
}
