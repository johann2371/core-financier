package com.corefi.service.impl;

import com.corefi.entity.CompteFinancier;
import com.corefi.service.interfaces.ICompteFinancierService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CompteFinancierServiceImpl implements ICompteFinancierService {

    @Override
    public List<CompteFinancier> findAll() {
        return null;
    }

    @Override
    public CompteFinancier findById(Long id) {
        return null;
    }
}
