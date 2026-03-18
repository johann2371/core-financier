package com.corefi.service.interfaces;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import java.util.List;

/**
 * ÉTAPE 1 — Interface de service (les controllers dépendent d'ici, jamais de l'impl)
 */
public interface ITiersService {

    TiersResponse creer(TiersCreateRequest request);

    TiersResponse findById(Long id);

    List<TiersResponse> findAll();

    List<TiersResponse> findByType(String type); // "CLIENT" ou "FOURNISSEUR"

    TiersResponse mettreAJour(Long id, TiersCreateRequest request);

    void desactiver(Long id);
}
