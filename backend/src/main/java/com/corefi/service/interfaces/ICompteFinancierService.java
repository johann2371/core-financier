package com.corefi.service.interfaces;

import com.corefi.entity.CompteFinancier;
import java.util.List;

public interface ICompteFinancierService {
    List<CompteFinancier> findAll();

    CompteFinancier findById(Long id);
}
