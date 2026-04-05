package com.corefi.service.interfaces;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import java.util.List;

public interface IDecaissementService {
    DecaissementResponse creer(DecaissementCreateRequest request);

    DecaissementResponse findById(Long id);

    List<DecaissementResponse> findAll();

    DecaissementResponse soumettre(Long id);

    DecaissementResponse validerRF(Long id, ValidationRFRequest request);

    DecaissementResponse rejeterRF(Long id, ValidationRFRequest request);

    DecaissementResponse approuverPDG(Long id, ApprobationPDGRequest request);

    DecaissementResponse rejeterPDG(Long id, ApprobationPDGRequest request);

    DecaissementResponse executer(Long id, ExecutionCaissierRequest request);
    
    DecaissementResponse updateStatut(Long id, String nouveauStatut, String commentaire);
}
