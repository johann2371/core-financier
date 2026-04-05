package com.corefi.service.impl;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;
import com.corefi.entity.Parametrage;
import com.corefi.repository.ParametrageRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.IParametrageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParametrageServiceImpl implements IParametrageService {

    private final ParametrageRepository parametrageRepository;
    private final IJournalAuditService journalAuditService;

    @Override
    public List<ParametrageResponse> getAll() {
        return parametrageRepository.findAll().stream()
                .map(p -> new ParametrageResponse(p.getId(), p.getCle(), p.getValeur(), p.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    public ParametrageResponse update(String cle, ParametrageUpdateRequest request) {
        Parametrage p = parametrageRepository.findByCle(cle)
                .orElse(new Parametrage(null, cle, "", "", null));
        
        String ancienneValeur = p.getValeur();
        p.setValeur(request.getValeur());
        if(request.getDescription() != null) {
            p.setDescription(request.getDescription());
        }
        
        Parametrage saved = parametrageRepository.save(p);
        
        journalAuditService.enregistrer("UPDATE", "Parametrage", saved.getId(),
                "{\"valeur\":\"" + ancienneValeur + "\"}",
                "{\"valeur\":\"" + request.getValeur() + "\"}",
                null); // Le principal est injecté dans le filtre pour l'audit
                
        return new ParametrageResponse(saved.getId(), saved.getCle(), saved.getValeur(), saved.getDescription());
    }
}
