package com.corefi.service.impl;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.service.interfaces.IFactureService;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureServiceImpl implements IFactureService {

    private final IJournalAuditService journalAuditService;

    @Override
    @Transactional
    public FactureResponse creer(FactureCreateRequest request) {
        return null;
    }

    @Override
    public FactureResponse findById(Long id) {
        return null;
    }

    @Override
    public List<FactureResponse> findAll() {
        return null;
    }

    @Override
    @Transactional
    public FactureResponse valider(Long id) {
        return null;
    }

    @Override
    @Transactional
    public FactureResponse annuler(Long id) {
        return null;
    }
}
