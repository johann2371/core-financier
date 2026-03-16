package com.corefi.service.interfaces;

import com.corefi.dto.request.encaissement.AffectationRequest;
import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import java.util.List;

public interface IEncaissementService {
    EncaissementResponse creer(EncaissementCreateRequest request);

    EncaissementResponse findById(Long id);

    List<EncaissementResponse> findAll();

    void affecter(Long encaissementId, List<AffectationRequest> affectations);
}
