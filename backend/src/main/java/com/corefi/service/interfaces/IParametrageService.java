package com.corefi.service.interfaces;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;

import java.util.List;

public interface IParametrageService {
    List<ParametrageResponse> getAll();
    ParametrageResponse update(String cle, ParametrageUpdateRequest request);
}
