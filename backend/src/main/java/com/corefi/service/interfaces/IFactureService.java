package com.corefi.service.interfaces;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import java.util.List;

public interface IFactureService {
    FactureResponse creer(FactureCreateRequest request);

    FactureResponse findById(Long id);

    List<FactureResponse> findAll();

    FactureResponse valider(Long id);

    FactureResponse annuler(Long id);
}
