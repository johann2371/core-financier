package com.corefi.service.impl;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.service.interfaces.IDecaissementService;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecaissementServiceImpl implements IDecaissementService {

    private final IJournalAuditService journalAuditService;

    @Override
    @Transactional
    public DecaissementResponse creer(DecaissementCreateRequest request) {
        return null;
    }

    @Override
    public DecaissementResponse findById(Long id) {
        return null;
    }

    @Override
    public List<DecaissementResponse> findAll() {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse soumettre(Long id) {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse validerRF(Long id, ValidationRFRequest request) {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse rejeterRF(Long id, ValidationRFRequest request) {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse approuverPDG(Long id, ApprobationPDGRequest request) {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse rejeterPDG(Long id, ApprobationPDGRequest request) {
        return null;
    }

    @Override
    @Transactional
    public DecaissementResponse executer(Long id, ExecutionCaissierRequest request) {
        return null;
    }
}
