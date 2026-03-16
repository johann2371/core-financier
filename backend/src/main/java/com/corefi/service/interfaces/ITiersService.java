package com.corefi.service.interfaces;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import java.util.List;

public interface ITiersService {
    TiersResponse creer(TiersCreateRequest request);

    TiersResponse findById(Long id);

    List<TiersResponse> findAll();

    TiersResponse mettreAJour(Long id, TiersCreateRequest request);
}
