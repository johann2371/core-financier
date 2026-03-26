package com.corefi.repository;

import com.corefi.entity.JustificatifDecaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JustificatifRepository extends JpaRepository<JustificatifDecaissement, Long> {
    List<JustificatifDecaissement> findByDecaissementId(Long decaissementId);
}
