package com.corefi.service.interfaces;

import com.corefi.dto.response.compte.CompteFinancierResponse;
import java.util.List;

public interface ICompteFinancierService {
    List<CompteFinancierResponse> findAll();
    CompteFinancierResponse findById(Long id);
}
