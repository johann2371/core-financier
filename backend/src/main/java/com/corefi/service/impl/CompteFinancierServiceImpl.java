package com.corefi.service.impl;

import com.corefi.dto.response.compte.CompteFinancierResponse;
import com.corefi.mapper.CompteFinancierMapper;
import com.corefi.service.interfaces.ICompteFinancierService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompteFinancierServiceImpl implements ICompteFinancierService {

    private final CompteFinancierMapper compteFinancierMapper;

    @Override
    public List<CompteFinancierResponse> findAll() {
        return null; // TODO: logique métier
    }

    @Override
    public CompteFinancierResponse findById(Long id) {
        return null; // TODO: logique métier
    }
}
