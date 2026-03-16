package com.corefi.service.impl;

import com.corefi.dto.request.encaissement.AffectationRequest;
import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.service.interfaces.IEncaissementService;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EncaissementServiceImpl implements IEncaissementService {

    private final IJournalAuditService journalAuditService;

    @Override
    @Transactional
    public EncaissementResponse creer(EncaissementCreateRequest request) {
        return null;
    }

    @Override
    public EncaissementResponse findById(Long id) {
        return null;
    }

    @Override
    public List<EncaissementResponse> findAll() {
        return null;
    }

    @Override
    @Transactional
    public void affecter(Long encaissementId, List<AffectationRequest> affectations) {
    }
}
