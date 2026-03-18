package com.corefi.service.impl;

import com.corefi.dto.response.compte.CompteFinancierResponse;
import com.corefi.entity.CompteFinancier;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.mapper.CompteFinancierMapper;
import com.corefi.repository.CompteFinancierRepository;
import com.corefi.service.interfaces.ICompteFinancierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompteFinancierServiceImpl implements ICompteFinancierService {

    private final CompteFinancierRepository compteFinancierRepository;
    private final CompteFinancierMapper compteFinancierMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CompteFinancierResponse> findAll() {
        List<CompteFinancier> comptes = compteFinancierRepository.findAll();

        // Initialiser les objets Lazy (devise)
        for (CompteFinancier c : comptes) {
            if (c.getDevise() != null) {
                c.getDevise().getCode();
            }
        }

        return comptes.stream()
                .map(compteFinancierMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompteFinancierResponse findById(Long id) {
        CompteFinancier compte = compteFinancierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte financier introuvable avec ID : " + id));

        // Initialiser les objets Lazy
        if (compte.getDevise() != null) {
            compte.getDevise().getCode();
        }

        return compteFinancierMapper.toResponse(compte);
    }
}
