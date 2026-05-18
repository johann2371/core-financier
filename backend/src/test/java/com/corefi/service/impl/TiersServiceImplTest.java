package com.corefi.service.impl;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.entity.Tiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.mapper.TiersMapper;
import com.corefi.repository.TiersRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TiersServiceImplTest {

    @Mock
    private TiersRepository tiersRepository;
    @Mock
    private TiersMapper tiersMapper;
    @Mock
    private IJournalAuditService journalAuditService;

    @InjectMocks
    private TiersServiceImpl tiersService;

    @Test
    @DisplayName("Doit lever une exception si le tiers à mettre à jour n'existe pas")
    void mettreAJour_nonExistentTiers_throwsResourceNotFoundException() {
        // given
        TiersCreateRequest request = new TiersCreateRequest();
        given(tiersRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tiersService.mettreAJour(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tiers introuvable");
    }

    @Test
    @DisplayName("Doit lever une exception si l'ID n'existe pas lors de la recherche")
    void findById_nonExistentTiers_throwsResourceNotFoundException() {
        // given
        given(tiersRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tiersService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tiers introuvable");
    }
}
