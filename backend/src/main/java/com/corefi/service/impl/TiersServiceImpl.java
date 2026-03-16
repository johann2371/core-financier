package com.corefi.service.impl;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.service.interfaces.ITiersService;
import com.corefi.service.interfaces.IJournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TiersServiceImpl implements ITiersService {

    private final IJournalAuditService journalAuditService;

    @Override
    @Transactional
    public TiersResponse creer(TiersCreateRequest request) {
        return null;
    }

    @Override
    public TiersResponse findById(Long id) {
        return null;
    }

    @Override
    public List<TiersResponse> findAll() {
        return null;
    }

    @Override
    @Transactional
    public TiersResponse mettreAJour(Long id, TiersCreateRequest request) {
        return null;
    }
}
